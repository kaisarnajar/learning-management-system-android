package com.darsequran.academy.data.remote

import com.darsequran.academy.data.model.AuthResponse
import com.darsequran.academy.data.model.GoogleLoginRequest
import com.darsequran.academy.data.model.LoginRequest
import com.darsequran.academy.data.model.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthResponse>

    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<AuthResponse>

    @POST("auth/google")
    suspend fun googleLogin(
        @Body request: GoogleLoginRequest
    ): Response<AuthResponse>
}
