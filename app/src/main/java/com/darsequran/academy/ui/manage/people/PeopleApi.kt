package com.darsequran.academy.ui.manage.people

import com.darsequran.academy.data.remote.Paged
import com.darsequran.academy.data.staff.ActionResponse
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

/*
 * The "people" part of the admin panel: teachers, enrollment requests, certificates, student reviews,
 * contact inquiries and the audit log (/api/v1/admin/...). Writes take the website forms' fields.
 */

// ------------------------------------------------------------------ teachers

@Serializable
data class TeacherRow(
    val id: String,
    val name: String = "",
    val email: String? = null,
    val specialization: String = "",
    val initials: String = "",
    val imageUrl: String? = null,
    val published: Boolean = true,
    val createdAt: String? = null,
)

@Serializable
data class TeacherCourse(
    val id: String,
    val title: String = "",
    val category: String? = null,
    val status: String = "",
    val statusLabel: String = "",
    val studentCount: Int = 0,
)

@Serializable
data class TeacherDetail(
    val id: String,
    val name: String = "",
    val email: String? = null,
    val specialization: String = "",
    val bio: String = "",
    val initials: String = "",
    val imageUrl: String? = null,
    val published: Boolean = true,
    val createdAt: String? = null,
    val courses: List<TeacherCourse> = emptyList(),
)

@Serializable data class TeacherResponse(val teacher: TeacherDetail)

/** An account check: ok with the account's name, or the reason it cannot be used. */
@Serializable
data class AccountLookup(val ok: Boolean = false, val name: String? = null, val email: String? = null, val error: String? = null)

@Serializable data class LookupResponse(val lookup: AccountLookup)

interface TeachersAdminApi {
    @GET("admin/teachers") suspend fun list(@Query("page") page: Int, @Query("q") q: String?): Paged<TeacherRow>
    @GET("admin/teachers/{id}") suspend fun get(@Path("id") id: String): TeacherResponse
    @GET("admin/teachers/lookup") suspend fun lookup(@Query("email") email: String, @Query("excludeTeacherId") excludeTeacherId: String?): LookupResponse
    @Multipart @POST("admin/teachers") suspend fun create(@Part parts: List<MultipartBody.Part>): ActionResponse
    @Multipart @PATCH("admin/teachers/{id}") suspend fun update(@Path("id") id: String, @Part parts: List<MultipartBody.Part>): ActionResponse
    @DELETE("admin/teachers/{id}") suspend fun delete(@Path("id") id: String): ActionResponse
}

// --------------------------------------------------------------- enrollments

@Serializable data class PersonRef(val id: String? = null, val name: String? = null, val email: String = "")
@Serializable data class CourseRef(val id: String, val title: String = "")

@Serializable
data class EnrollmentRequest(
    val id: String,
    val courseId: String,
    val courseTitle: String = "",
    val status: String = "",
    val createdAt: String? = null,
    val user: PersonRef = PersonRef(),
)

/** The same list answer read for its extras: tab counts and the courses the enrol form offers. */
@Serializable
data class EnrollmentsSummary(
    val freeCount: Int = 0,
    val paidCount: Int = 0,
    val enrollableCourses: List<CourseRef> = emptyList(),
)

interface EnrollmentsAdminApi {
    @GET("admin/enrollments")
    suspend fun list(@Query("page") page: Int, @Query("q") q: String?, @Query("type") type: String?): Paged<EnrollmentRequest>

    @GET("admin/enrollments")
    suspend fun summary(@Query("type") type: String? = null, @Query("pageSize") pageSize: Int = 1): EnrollmentsSummary

    @GET("admin/enrollments/lookup") suspend fun lookup(@Query("email") email: String): LookupResponse
    @Multipart @POST("admin/enrollments") suspend fun enroll(@Part parts: List<MultipartBody.Part>): ActionResponse
    @Multipart @POST("admin/enrollments/{id}/approve") suspend fun approve(@Path("id") id: String, @Part parts: List<MultipartBody.Part>): ActionResponse
    @Multipart @POST("admin/enrollments/{id}/reject") suspend fun reject(@Path("id") id: String, @Part parts: List<MultipartBody.Part>): ActionResponse
}

// --------------------------------------------------------------- certificates

@Serializable
data class CertificateRow(
    val enrollmentId: String,
    val certificateNumber: String? = null,
    val certificateGeneratedAt: String? = null,
    val certificateType: String? = null,
    val typeLabel: String = "",
    val certificateGrade: Int? = null,
    val certificateEmailSentAt: String? = null,
    val user: PersonRef = PersonRef(),
    val course: CourseRef = CourseRef(""),
)

@Serializable data class CertificateResponse(val certificate: CertificateRow)

@Serializable
data class CertificateCandidate(val id: String, val user: PersonRef = PersonRef(), val course: CourseRef = CourseRef(""))

@Serializable data class CertificateCandidates(val enrollments: List<CertificateCandidate> = emptyList())

interface CertificatesAdminApi {
    @GET("admin/certificates") suspend fun list(@Query("page") page: Int, @Query("q") q: String?): Paged<CertificateRow>
    @GET("admin/certificates/eligible") suspend fun eligible(): CertificateCandidates
    @GET("admin/certificates/{id}") suspend fun get(@Path("id") enrollmentId: String): CertificateResponse
    @Multipart @POST("admin/certificates") suspend fun generate(@Part parts: List<MultipartBody.Part>): ActionResponse
    @DELETE("admin/certificates/{id}") suspend fun delete(@Path("id") enrollmentId: String): ActionResponse
    @Streaming @GET("admin/certificates/{id}/pdf") suspend fun pdf(@Path("id") enrollmentId: String): ResponseBody
    @POST("admin/certificates/{id}/email") suspend fun email(@Path("id") enrollmentId: String): ActionResponse
}

// --------------------------------------------------------------- reviews

@Serializable
data class StudentReviewRow(
    val id: String,
    val quote: String = "",
    val course: String? = null,
    val courseLabel: String = "",
    val location: String? = null,
    val rating: Int = 0,
    val status: String = "",
    val statusLabel: String = "",
    val featuredOnHomepage: Boolean = false,
    val createdAt: String? = null,
    val user: PersonRef = PersonRef(),
)

@Serializable
data class ReviewsSummary(val pendingCount: Int = 0, val approvedCount: Int = 0, val featuredCount: Int = 0, val featuredMax: Int = 0)

@Serializable
data class ReviewResponse(val review: StudentReviewRow, val featuredCount: Int = 0, val featuredMax: Int = 0, val canFeature: Boolean = false)

interface ReviewsAdminApi {
    @GET("admin/review-approvals")
    suspend fun list(@Query("page") page: Int, @Query("q") q: String?, @Query("type") type: String?): Paged<StudentReviewRow>

    @GET("admin/review-approvals")
    suspend fun summary(@Query("pageSize") pageSize: Int = 1): ReviewsSummary

    @GET("admin/review-approvals/{id}") suspend fun get(@Path("id") id: String): ReviewResponse
    @Multipart @POST("admin/review-approvals/{id}/approve") suspend fun approve(@Path("id") id: String, @Part parts: List<MultipartBody.Part>): ActionResponse
    @POST("admin/review-approvals/{id}/reject") suspend fun reject(@Path("id") id: String): ActionResponse
    @Multipart @PATCH("admin/review-approvals/{id}") suspend fun saveHomepage(@Path("id") id: String, @Part parts: List<MultipartBody.Part>): ActionResponse
    @DELETE("admin/review-approvals/{id}") suspend fun delete(@Path("id") id: String): ActionResponse
}

// --------------------------------------------------------------- contact inquiries

@Serializable data class Replier(val name: String? = null, val email: String? = null)

@Serializable
data class ContactInquiry(
    val id: String,
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val message: String = "",
    val reply: String? = null,
    val status: String = "",
    val statusLabel: String = "",
    val repliedAt: String? = null,
    val repliedBy: Replier? = null,
    val createdAt: String? = null,
)

@Serializable data class ContactInquiryResponse(val inquiry: ContactInquiry)

interface ContactInquiriesApi {
    @GET("admin/contact-inquiries")
    suspend fun list(@Query("page") page: Int, @Query("q") q: String?, @Query("filter") filter: String?): Paged<ContactInquiry>

    @GET("admin/contact-inquiries/{id}") suspend fun get(@Path("id") id: String): ContactInquiryResponse
    @Multipart @POST("admin/contact-inquiries/{id}/reply") suspend fun reply(@Path("id") id: String, @Part parts: List<MultipartBody.Part>): ActionResponse
    @DELETE("admin/contact-inquiries/{id}") suspend fun delete(@Path("id") id: String): ActionResponse
}

// --------------------------------------------------------------- audit log

@Serializable
data class AuditEntry(
    val id: String,
    val createdAt: String? = null,
    val actor: String = "system",
    val action: String = "",
    val entity: String = "",
    val entityId: String? = null,
    val reason: String? = null,
    val before: JsonElement? = null,
    val after: JsonElement? = null,
)

@Serializable data class AuditEntities(val entities: List<String> = emptyList())

interface AuditLogApi {
    @GET("admin/audit-log")
    suspend fun list(
        @Query("page") page: Int,
        @Query("entity") entity: String?,
        @Query("entityId") entityId: String?,
        @Query("actor") actor: String?,
    ): Paged<AuditEntry>

    @GET("admin/audit-log") suspend fun entities(@Query("pageSize") pageSize: Int = 1): AuditEntities
}
