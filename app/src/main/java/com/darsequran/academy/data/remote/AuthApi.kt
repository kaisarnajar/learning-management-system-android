package com.darsequran.academy.data.remote

import com.darsequran.academy.data.model.AuthResponse
import com.darsequran.academy.data.model.CoursesResponse
import com.darsequran.academy.data.model.CreateReviewRequest
import com.darsequran.academy.data.model.DailyInspirationResponse
import com.darsequran.academy.data.model.DeviceTokenRequest
import com.darsequran.academy.data.model.EnrollmentsResponse
import com.darsequran.academy.data.model.GoogleLoginRequest
import com.darsequran.academy.data.model.LoginRequest
import com.darsequran.academy.data.model.MarkReadRequest
import com.darsequran.academy.data.model.NotificationsResponse
import com.darsequran.academy.data.model.PaymentHistoryResponse
import com.darsequran.academy.data.model.PaymentSettingsResponse
import com.darsequran.academy.data.model.RegisterRequest
import com.darsequran.academy.data.model.StudentAttendanceResponse
import com.darsequran.academy.data.model.StudentGradesResponse
import com.darsequran.academy.data.model.StudentReviewsResponse
import com.darsequran.academy.data.model.SubmitPaymentRequest
import com.darsequran.academy.data.model.UpdateProfileRequest
import com.darsequran.academy.data.model.UserProfileResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
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

    @GET("notifications")
    suspend fun getNotifications(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("filter") filter: String = "all"
    ): Response<NotificationsResponse>

    @PATCH("notifications")
    suspend fun markNotificationsRead(
        @Body request: MarkReadRequest
    ): Response<NotificationsResponse>

    @POST("notifications/device-token")
    suspend fun registerDeviceToken(
        @Body request: DeviceTokenRequest
    ): Response<AuthResponse>

    @GET("student/attendance")
    suspend fun getStudentAttendance(
        @Query("courseId") courseId: String? = null
    ): Response<StudentAttendanceResponse>

    @GET("student/grades")
    suspend fun getStudentGrades(
        @Query("courseId") courseId: String? = null
    ): Response<StudentGradesResponse>

    @GET("payment-settings")
    suspend fun getPaymentSettings(): Response<PaymentSettingsResponse>

    @GET("payments/history")
    suspend fun getPaymentHistory(): Response<PaymentHistoryResponse>

    @POST("payments/submit")
    suspend fun submitPayment(
        @Body request: SubmitPaymentRequest
    ): Response<AuthResponse>

    @GET("reviews")
    suspend fun getReviews(): Response<StudentReviewsResponse>

    @POST("reviews")
    suspend fun submitReview(
        @Body request: CreateReviewRequest
    ): Response<AuthResponse>

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

    @GET("library")
    suspend fun getLibraryBooks(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("search") search: String? = null,
        @Query("topic") topic: String? = null
    ): Response<com.darsequran.academy.data.model.LibraryResponse>

    @GET("announcements")
    suspend fun getAnnouncements(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("search") search: String? = null
    ): Response<com.darsequran.academy.data.model.AnnouncementsResponse>

    @GET("blog")
    suspend fun getBlogPosts(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("search") search: String? = null
    ): Response<com.darsequran.academy.data.model.BlogResponse>

    @GET("teachers")
    suspend fun getTeachers(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("search") search: String? = null
    ): Response<com.darsequran.academy.data.model.TeachersResponse>

    @GET("fatwa")
    suspend fun getFatwas(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("search") search: String? = null,
        @Query("category") category: String? = null
    ): Response<com.darsequran.academy.data.model.FatwaResponse>

    @POST("fatwa")
    suspend fun submitFatwaQuery(
        @Body request: com.darsequran.academy.data.model.SubmitFatwaRequest
    ): Response<AuthResponse>

    @GET("bookstore")
    suspend fun getBookstoreItems(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("search") search: String? = null
    ): Response<com.darsequran.academy.data.model.BookstoreResponse>
}
