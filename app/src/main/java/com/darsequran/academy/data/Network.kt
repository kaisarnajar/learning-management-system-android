package com.darsequran.academy.data

import com.darsequran.academy.BuildConfig
import com.darsequran.academy.data.remote.AcademyApi
import com.darsequran.academy.data.remote.RefreshApi
import com.darsequran.academy.data.remote.RefreshRequest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.Dispatcher
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/** Absolute URL for an image or file path the API returns ("/uploads/x.png" or "https://..."). */
fun resolveUrl(path: String?): String? = when {
    path.isNullOrBlank() -> null
    path.startsWith("http://") || path.startsWith("https://") -> path
    path.startsWith("/") -> CurrentEnvironment.baseUrl + path
    else -> CurrentEnvironment.baseUrl + "/" + path
}

/** Retrofit needs a fixed base URL; requests are pointed at the current environment by [environmentHost]. */
private const val PLACEHOLDER_BASE = "https://api.invalid/api/v1/"

/**
 * An access token this close to running out is refreshed before a request is sent: sending it would only earn a 401
 * and a second round trip. The margin also covers the time between the server issuing the token and the app saving it.
 */
private const val REFRESH_AHEAD_MS = 60_000L

class Network(
    private val sessionStore: SessionSource,
    val json: Json,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val refreshMutex = Mutex()

    private val logging = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
    }

    /** Sends API calls to the environment chosen now (production or staging). */
    private val environmentHost = Interceptor { chain ->
        val request = chain.request()
        if (request.url.host != "api.invalid") return@Interceptor chain.proceed(request)
        val base = CurrentEnvironment.baseUrl.toHttpUrl()
        val url = request.url.newBuilder().scheme(base.scheme).host(base.host).port(base.port).build()
        chain.proceed(request.newBuilder().url(url).build())
    }

    private val baseClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(70, TimeUnit.SECONDS) // receipts are rendered on demand
        .addInterceptor(environmentHost)
        .addInterceptor(logging)
        .build()

    // Its own dispatcher: requests waiting in the authenticator hold the shared client's per-host slots (5), so a
    // refresh queued behind them would never run and every request would hang.
    private val refreshApi: RefreshApi =
        retrofit(baseClient.newBuilder().dispatcher(Dispatcher()).build()).create(RefreshApi::class.java)

    /**
     * Adds the Bearer token when signed in (a retry already carries the fresh one), but only for the academy's own
     * server: images and files can live elsewhere (the public upload bucket, Google avatars) and must not see it.
     * A token about to run out is refreshed first (see [REFRESH_AHEAD_MS]); if that fails the current one is sent and
     * a 401 goes through [refreshAuthenticator] as before.
     */
    private val authHeader = Interceptor { chain ->
        val original = chain.request()
        if (original.header("Authorization") != null || !isAcademyServer(original.url)) {
            return@Interceptor chain.proceed(original)
        }
        // A session belongs to one environment: during a switch the old one must not reach the new server.
        val session = currentSession()
        val token = when {
            session == null -> null
            session.expiresAt != null && clock() >= session.expiresAt - REFRESH_AHEAD_MS ->
                // Refreshed, or else whatever is current now (null when the refresh token was rejected: signed out).
                freshToken(session.accessToken) ?: currentSession()?.accessToken
            else -> session.accessToken
        }
        if (token == null) return@Interceptor chain.proceed(original)
        chain.proceed(original.newBuilder().header("Authorization", "Bearer $token").build())
    }

    private fun currentSession(): Session? = sessionStore.session.value?.takeIf { it.env == CurrentEnvironment.value }

    /**
     * The access token to use instead of [usedToken]: refreshes the pair, unless another request already did so while
     * this one waited. Refresh tokens are single use, so callers share one refresh behind a mutex. Null when there is
     * no session for the current environment or the refresh failed.
     */
    private fun freshToken(usedToken: String?): String? = runBlocking {
        refreshMutex.withLock {
            val current = sessionStore.session.value ?: return@withLock null
            if (current.env != CurrentEnvironment.value) return@withLock null
            // Someone else already refreshed while this request waited.
            if (current.accessToken != usedToken) return@withLock current.accessToken
            try {
                val tokens = refreshApi.refresh(RefreshRequest(current.refreshToken))
                sessionStore.saveRefreshed(current, tokens)
                // Switched environment while refreshing: don't send to the old server.
                if (current.env != CurrentEnvironment.value) return@withLock null
                tokens.accessToken
            } catch (e: HttpException) {
                if (e.code() == HTTP_UNAUTHORIZED) sessionStore.expire(current)
                null
            } catch (e: Exception) {
                null
            }
        }
    }

    /** On a 401, refresh the token pair once ([freshToken]) and retry with the new access token. */
    private val refreshAuthenticator = object : Authenticator {
        override fun authenticate(route: Route?, response: Response): Request? {
            if (response.request.header("Authorization") == null) return null
            if (responseCount(response) >= 2) return null
            // A request from before an environment switch: its 401 says nothing about the current session, and
            // retrying would send the new environment's token to the old server.
            if (response.request.url.host != CurrentEnvironment.baseUrl.toHttpUrl().host) return null
            val usedToken = response.request.header("Authorization")?.removePrefix("Bearer ")
            val token = freshToken(usedToken) ?: return null
            return response.request.newBuilder().header("Authorization", "Bearer $token").build()
        }
    }

    private fun isAcademyServer(url: HttpUrl): Boolean {
        val base = CurrentEnvironment.baseUrl.toHttpUrl()
        return url.scheme == base.scheme && url.host == base.host && url.port == base.port
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) { count++; prior = prior.priorResponse }
        return count
    }

    val client: OkHttpClient = baseClient.newBuilder()
        .addInterceptor(authHeader)
        .authenticator(refreshAuthenticator)
        .build()

    private val retrofit: Retrofit = retrofit(client)

    val api: AcademyApi = retrofit.create(AcademyApi::class.java)

    private val services = ConcurrentHashMap<Class<*>, Any>()

    /**
     * A Retrofit service for one part of the API (the admin and teacher areas each have their own). Made once per
     * interface: screens ask for one on every call, and each proxy re-reads the interface's annotations.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> create(service: Class<T>): T = services.getOrPut(service) { retrofit.create(service) } as T

    private fun retrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(PLACEHOLDER_BASE)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}
