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

class Network(private val sessionStore: SessionStore, val json: Json) {
    private val refreshMutex = Mutex()

    private val logging = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
    }

    /** Sends API calls to the environment chosen now (production, staging or a local server). */
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

    private val refreshApi: RefreshApi = retrofit(baseClient).create(RefreshApi::class.java)

    /** Adds the Bearer token to every call when signed in (a retry already carries the fresh one). */
    private val authHeader = Interceptor { chain ->
        val original = chain.request()
        val token = sessionStore.session.value?.accessToken
        val request = if (token != null && original.header("Authorization") == null) {
            original.newBuilder().header("Authorization", "Bearer $token").build()
        } else original
        chain.proceed(request)
    }

    /**
     * On a 401, refresh the token pair once and retry. Refresh tokens are single
     * use, so concurrent 401s share one refresh behind a mutex: a request that
     * waited simply retries with the token the first one obtained.
     */
    private val refreshAuthenticator = object : Authenticator {
        override fun authenticate(route: Route?, response: Response): Request? {
            if (response.request.header("Authorization") == null) return null
            if (responseCount(response) >= 2) return null
            // A request from before an environment switch: its 401 says nothing about the current session, and
            // retrying would send the new environment's token to the old server.
            if (response.request.url.host != CurrentEnvironment.baseUrl.toHttpUrl().host) return null
            val usedToken = response.request.header("Authorization")?.removePrefix("Bearer ")
            return runBlocking {
                refreshMutex.withLock {
                    val current = sessionStore.session.value ?: return@withLock null
                    if (current.accessToken != usedToken) {
                        // Someone else already refreshed while this request waited.
                        return@withLock response.request.newBuilder().header("Authorization", "Bearer ${current.accessToken}").build()
                    }
                    try {
                        val tokens = refreshApi.refresh(RefreshRequest(current.refreshToken))
                        sessionStore.save(tokens)
                        response.request.newBuilder().header("Authorization", "Bearer ${tokens.accessToken}").build()
                    } catch (e: HttpException) {
                        if (e.code() == 401) sessionStore.clear()
                        null
                    } catch (e: Exception) {
                        null
                    }
                }
            }
        }
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

    /** A Retrofit service for one part of the API (the admin and teacher areas each have their own). */
    fun <T> create(service: Class<T>): T = retrofit.create(service)

    private fun retrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(PLACEHOLDER_BASE)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}
