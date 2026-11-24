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
    path.startsWith("/") -> BuildConfig.BASE_URL.trimEnd('/') + path
    else -> BuildConfig.BASE_URL.trimEnd('/') + "/" + path
}

class Network(private val sessionStore: SessionStore, val json: Json) {
    private val refreshMutex = Mutex()

    private val logging = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
    }

    private val baseClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(70, TimeUnit.SECONDS) // receipts are rendered on demand
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

    val api: AcademyApi = retrofit(client).create(AcademyApi::class.java)

    private fun retrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL.trimEnd('/') + "/api/v1/")
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}
