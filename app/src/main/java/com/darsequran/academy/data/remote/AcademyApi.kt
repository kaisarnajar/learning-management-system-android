package com.darsequran.academy.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

/** /api/v1. Every call goes through [com.darsequran.academy.data.ApiCall] for error handling. */
interface AcademyApi {

    // ---- auth & account
    @POST("auth/login") suspend fun login(@Body body: LoginRequest): TokenResponse
    @POST("auth/register") suspend fun register(@Body body: RegisterRequest): TokenResponse
    @POST("auth/google") suspend fun google(@Body body: GoogleRequest): TokenResponse
    @POST("auth/logout") suspend fun logout(@Body body: RefreshRequest): OkResponse
    @POST("auth/forgot-password") suspend fun forgotPassword(@Body body: EmailRequest): OkResponse
    @POST("auth/resend-verification") suspend fun resendVerification(@Body body: EmailRequest): OkResponse
    @GET("auth/me") suspend fun me(): ProfileResponse
    @PUT("auth/me") suspend fun updateProfile(@Body body: ProfileUpdate): ProfileResponse
    @HTTP(method = "DELETE", path = "auth/me", hasBody = true) suspend fun deleteAccount(@Body body: DeleteAccountRequest): OkResponse
    @Multipart @POST("auth/me/photo") suspend fun uploadPhoto(@Part image: MultipartBody.Part): PhotoResponse
    @DELETE("auth/me/photo") suspend fun removePhoto(): PhotoResponse
    @POST("auth/change-password") suspend fun changePassword(@Body body: ChangePasswordRequest): OkResponse

    // ---- public content
    @GET("app-config") suspend fun appConfig(): AppConfig
    @GET("home") suspend fun home(): HomeResponse
    @GET("courses") suspend fun courses(@Query("page") page: Int, @Query("search") search: String?, @Query("category") category: String? = null): Paged<Course>
    @GET("courses/{id}") suspend fun course(@Path("id") id: String): CourseDetailResponse
    @GET("teachers") suspend fun teachers(@Query("page") page: Int, @Query("search") search: String?): Paged<Teacher>
    @GET("teachers/{id}") suspend fun teacher(@Path("id") id: String): TeacherDetailResponse
    @GET("blog") suspend fun blog(@Query("page") page: Int, @Query("search") search: String?): Paged<BlogPost>
    @GET("blog/{id}") suspend fun blogPost(@Path("id") id: String): BlogPostResponse
    @GET("blog/{id}/comments") suspend fun blogComments(@Path("id") id: String, @Query("skip") skip: Int): CommentsPage
    @POST("blog/{id}/comments") suspend fun addComment(@Path("id") id: String, @Body body: ContentRequest): CommentResponse
    @POST("blog/{id}/like") suspend fun toggleLike(@Path("id") id: String): LikeResponse
    @DELETE("blog/comments/{id}") suspend fun deleteComment(@Path("id") id: String): OkResponse
    @GET("fatwa") suspend fun fatwas(@Query("page") page: Int, @Query("search") search: String?, @Query("category") category: String?): Paged<Fatwa>
    @GET("fatwa/{id}") suspend fun fatwa(@Path("id") id: String): FatwaResponse
    @POST("fatwa") suspend fun askFatwa(@Body body: AskFatwaRequest): OkResponse
    @GET("library") suspend fun library(@Query("page") page: Int, @Query("search") search: String?): Paged<LibraryItem>
    @GET("library/{id}") suspend fun libraryItem(@Path("id") id: String): LibraryItemResponse
    @GET("bookstore") suspend fun books(@Query("page") page: Int, @Query("search") search: String?): Paged<Book>
    @GET("bookstore/{id}") suspend fun book(@Path("id") id: String): BookResponse
    @GET("announcements") suspend fun announcements(@Query("page") page: Int, @Query("search") search: String?): Paged<SiteAnnouncement>
    @GET("announcements/{id}") suspend fun announcement(@Path("id") id: String): AnnouncementResponse
    @GET("payment-settings") suspend fun paymentSettings(): PaymentSettingsResponse
    @POST("contact") suspend fun contact(@Body body: ContactRequest): OkResponse

    // ---- student
    @GET("enrollments") suspend fun enrollments(): EnrollmentsResponse
    @POST("enrollments") suspend fun enroll(@Body body: EnrollRequest): EnrollResponse
    @GET("courses/{id}/announcements") suspend fun courseAnnouncements(@Path("id") id: String, @Query("pageSize") pageSize: Int = 50): CourseAnnouncementsResponse
    @GET("student/attendance") suspend fun attendance(@Query("courseId") courseId: String?): AttendanceResponse
    @GET("student/grades") suspend fun grades(@Query("courseId") courseId: String?): GradesResponse
    @GET("payments/quote") suspend fun paymentQuote(@Query("courseId") courseId: String, @Query("type") type: String): PaymentQuote
    @Multipart @POST("payments/submit") suspend fun submitPayment(@PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>, @Part screenshot: MultipartBody.Part?): OkResponse
    @GET("payments/history") suspend fun paymentHistory(): PaymentHistory
    @Streaming @GET("payments/{id}/receipt") suspend fun receipt(@Path("id") id: String): ResponseBody
    @GET("waiver-requests") suspend fun waiverRequests(): WaiverRequestsResponse
    @POST("waiver-requests") suspend fun requestWaiver(@Body body: WaiverRequestBody): OkResponse
    @GET("reviews") suspend fun myReviews(): ReviewsResponse
    @POST("reviews") suspend fun addReview(@Body body: ReviewRequest): OkResponse

    // ---- bookstore
    @GET("bookstore/cart") suspend fun cart(): CartResponse
    @PATCH("bookstore/cart") suspend fun setCartLine(@Body body: CartLine): CartResponse
    @POST("bookstore/checkout-quote") suspend fun checkoutQuote(@Body body: CartReplace): CheckoutQuote
    @Multipart @POST("bookstore/orders") suspend fun placeOrder(@PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>, @Part screenshot: MultipartBody.Part?): OkResponse
    @GET("bookstore/orders") suspend fun orders(): OrdersResponse
    @GET("bookstore/orders/{id}") suspend fun order(@Path("id") id: String): OrderResponse

    // ---- notifications
    @GET("notifications") suspend fun notifications(@Query("page") page: Int, @Query("filter") filter: String = "all"): Paged<Notification>
    @GET("notifications/{id}") suspend fun notification(@Path("id") id: String): NotificationResponse
    @PATCH("notifications") suspend fun markRead(@Body body: MarkReadRequest): OkResponse
    @POST("notifications/device-token") suspend fun registerDevice(@Body body: DeviceTokenRequest): OkResponse
}

/** The refresh call lives on its own client without the authenticator, so it can never recurse. */
interface RefreshApi {
    @POST("auth/refresh") suspend fun refresh(@Body body: RefreshRequest): TokenResponse
}
