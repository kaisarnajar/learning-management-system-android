package com.darsequran.academy.data.remote

import com.darsequran.academy.data.local.TokenManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    const val PRODUCTION_BASE_URL = "https://staging-learning-management-system-teal-ten.vercel.app/api/v1/"
    const val EMULATOR_BASE_URL = "http://10.0.2.2:3000/api/v1/"

    // Default active base URL (Can be changed to EMULATOR_BASE_URL during local testing)
    var currentBaseUrl: String = PRODUCTION_BASE_URL

    private fun createOkHttpClient(tokenManager: TokenManager): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenManager))
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun getAuthApi(tokenManager: TokenManager): AuthApi {
        return Retrofit.Builder()
            .baseUrl(currentBaseUrl)
            .client(createOkHttpClient(tokenManager))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java)
    }
}
