package com.darsequran.academy.data.remote

import com.darsequran.academy.data.local.TokenManager
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()

        // Synchronously fetch token from DataStore for OkHttp interceptor
        val token = runBlocking { tokenManager.authTokenFlow.firstOrNull() }

        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        requestBuilder.addHeader("Accept", "application/json")
        requestBuilder.addHeader("Content-Type", "application/json")

        val response = chain.proceed(requestBuilder.build())

        if (response.code == 401) {
            // Token expired or invalid -> Clear local session
            runBlocking { tokenManager.clearSession() }
        }

        return response
    }
}
