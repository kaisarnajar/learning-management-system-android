package com.darsequran.academy.data.remote

import com.darsequran.academy.data.model.AuthResponse
import com.darsequran.academy.data.model.CoursesResponse
import com.darsequran.academy.data.model.DailyInspirationResponse
import com.darsequran.academy.data.model.EnrollmentsResponse
import com.darsequran.academy.data.model.GoogleLoginRequest
import com.darsequran.academy.data.model.LoginRequest
import com.darsequran.academy.data.model.RegisterRequest
import com.darsequran.academy.data.model.UpdateProfileRequest
import com.darsequran.academy.data.model.UserProfileResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

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

    @GET("auth/me")
    suspend fun getProfile(): Response<UserProfileResponse>

    @PUT("auth/me")
    suspend fun updateProfile(
        @Body request: UpdateProfileRequest
    ): Response<UserProfileResponse>

    @GET("daily-inspiration")
    suspend fun getDailyInspiration(): Response<DailyInspirationResponse>

    @GET("courses")
    suspend fun getCourses(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("search") search: String? = null
    ): Response<CoursesResponse>

    @GET("enrollments")
    suspend fun getEnrollments(): Response<EnrollmentsResponse>
}
