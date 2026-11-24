package com.darsequran.academy.ui.manage.content

import com.darsequran.academy.data.remote.Paged
import com.darsequran.academy.data.staff.ActionResponse
import com.darsequran.academy.ui.common.Tone
import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/*
 * Content management: site announcements, blogs and blog approvals, fatwa Q&A, the digital library and the
 * verse & hadith of the day (/api/v1/admin/...), and the teacher's own blog posts and fatwa answers
 * (/api/v1/teacher/...). Writes run the website's server actions, so their bodies are the website forms' fields.
 */

// ------------------------------------------------------------------ shared

@Serializable
data class ContentImage(val id: String, val imagePath: String, val caption: String? = null, val sortOrder: Int? = null)

/** Homepage slots and image limits, read from a list endpoint for the "new" forms. */
@Serializable
data class ContentLimits(val featuredCount: Int = 0, val featuredMax: Int = 4, val maxImages: Int = 5)

// ------------------------------------------------------------- announcements

@Serializable
data class AnnouncementRow(
    val id: String,
    val title: String = "",
    val eventDate: String? = null,
    val location: String? = null,
    val published: Boolean = false,
    val showOnHomepage: Boolean = false,
    val statusLabel: String = "",
    val imageCount: Int = 0,
    val thumbnail: String? = null,
    val createdBy: String? = null,
    val createdAt: String? = null,
)

@Serializable
data class AnnouncementDetail(
    val id: String,
    val title: String = "",
    val body: String = "",
    val eventDate: String? = null,
    val eventDateInput: String? = null,
    val location: String? = null,
    val published: Boolean = true,
    val showOnHomepage: Boolean = false,
    val statusLabel: String = "",
    val images: List<ContentImage> = emptyList(),
    val createdBy: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val publicPath: String? = null,
)

@Serializable
data class AnnouncementResponse(val announcement: AnnouncementDetail, val featuredCount: Int = 0, val featuredMax: Int = 4, val maxImages: Int = 5)

// --------------------------------------------------------------------- blogs

@Serializable
data class BlogRow(
    val id: String,
    val title: String = "",
    val excerpt: String? = null,
    val approvalStatus: String = "",
    val published: Boolean = false,
    val statusLabel: String = "",
    val featuredOnHomepage: Boolean = false,
    val imageCount: Int = 0,
    val thumbnail: String? = null,
    val authorName: String? = null,
    val authorEmail: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val publicPath: String? = null,
    val canEdit: Boolean = false,
    val canDelete: Boolean = false,
)

@Serializable
data class BlogDetail(
    val id: String,
    val title: String = "",
    val excerpt: String? = null,
    val body: String = "",
    val approvalStatus: String = "",
    val published: Boolean = false,
    val statusLabel: String = "",
    val featuredOnHomepage: Boolean = false,
    val images: List<ContentImage> = emptyList(),
    val authorName: String? = null,
    val authorEmail: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val publicPath: String? = null,
    /** Admin: a teacher's submission, whose text and images cannot be changed. */
    val contentReadOnly: Boolean = false,
    /** Teacher: false once the post is approved. */
    val canEdit: Boolean = true,
    val canDelete: Boolean = true,
)

@Serializable
data class BlogResponse(val post: BlogDetail, val featuredCount: Int = 0, val featuredMax: Int = 4, val maxImages: Int = 10)

// --------------------------------------------------------------------- fatwa

@Serializable
data class FatwaRow(
    val id: String,
    val title: String = "",
    val category: String = "",
    val approvalStatus: String = "",
    val answered: Boolean = false,
    val statusLabel: String = "",
    val featuredOnHomepage: Boolean = false,
    val createdAt: String? = null,
    val answeredAt: String? = null,
    val publicPath: String? = null,
    val askerName: String? = null,
    val askerEmail: String? = null,
    val anonymous: Boolean = false,
)

@Serializable
data class FatwaDetail(
    val id: String,
    val title: String = "",
    val category: String = "",
    val approvalStatus: String = "",
    val answered: Boolean = false,
    val statusLabel: String = "",
    val featuredOnHomepage: Boolean = false,
    val createdAt: String? = null,
    val answeredAt: String? = null,
    val publicPath: String? = null,
    val askerName: String? = null,
    val askerEmail: String? = null,
    val anonymous: Boolean = false,
    val question: String = "",
    val answer: String? = null,
    /** Teacher: false while the answer awaits review and once it is approved. */
    val canEdit: Boolean = true,
)

@Serializable
data class FatwaResponse(val question: FatwaDetail, val featuredCount: Int = 0, val featuredMax: Int = 4)

// ------------------------------------------------------------------- library

@Serializable
data class LibraryItemDto(
    val id: String,
    val title: String = "",
    val author: String = "",
    val topic: String = "",
    val level: String = "Beginner",
    val language: String = "",
    val pdfUrl: String? = null,
    val imagePath: String? = null,
    val published: Boolean = true,
    val statusLabel: String = "",
    val featuredOnHomepage: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class LibraryOptions(
    val topics: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val levels: List<String> = listOf("Beginner", "Intermediate", "Advanced"),
    val featuredCount: Int = 0,
    val featuredMax: Int = 4,
)

@Serializable
data class LibraryResponse(
    val item: LibraryItemDto,
    val topics: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val levels: List<String> = listOf("Beginner", "Intermediate", "Advanced"),
    val featuredCount: Int = 0,
    val featuredMax: Int = 4,
) {
    val options get() = LibraryOptions(topics, languages, levels, featuredCount, featuredMax)
}

// ------------------------------------------------------------ verse & hadith

@Serializable
data class InspirationDto(
    val id: String,
    val kind: String = "QURAN",
    val arabicText: String = "",
    val englishTranslation: String = "",
    val reference: String? = null,
    val published: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val kindLabel: String = "",
    val statusLabel: String = "",
    val onHomepage: Boolean = false,
)

@Serializable data class InspirationResponse(val item: InspirationDto)

// ----------------------------------------------------------------------- API

interface ContentApi {
    // Site announcements
    @GET("admin/announcements") suspend fun announcements(@Query("page") page: Int, @Query("q") q: String?): Paged<AnnouncementRow>
    @GET("admin/announcements?pageSize=1") suspend fun announcementLimits(): ContentLimits
    @GET("admin/announcements/{id}") suspend fun announcement(@Path("id") id: String): AnnouncementResponse
    @Multipart @POST("admin/announcements") suspend fun createAnnouncement(@Part parts: List<MultipartBody.Part>): ActionResponse
    @Multipart @PATCH("admin/announcements/{id}") suspend fun updateAnnouncement(@Path("id") id: String, @Part parts: List<MultipartBody.Part>): ActionResponse
    @DELETE("admin/announcements/{id}") suspend fun deleteAnnouncement(@Path("id") id: String): ActionResponse
    @POST("admin/announcements/{id}/homepage") suspend fun toggleAnnouncementHomepage(@Path("id") id: String): ActionResponse

    // Blogs (admin)
    @GET("admin/blogs") suspend fun blogs(@Query("page") page: Int, @Query("q") q: String?): Paged<BlogRow>
    @GET("admin/blogs?pageSize=1") suspend fun blogLimits(): ContentLimits
    @GET("admin/blogs/{id}") suspend fun blog(@Path("id") id: String): BlogResponse
    @Multipart @POST("admin/blogs") suspend fun createBlog(@Part parts: List<MultipartBody.Part>): ActionResponse
    @Multipart @PATCH("admin/blogs/{id}") suspend fun updateBlog(@Path("id") id: String, @Part parts: List<MultipartBody.Part>): ActionResponse
    @DELETE("admin/blogs/{id}") suspend fun deleteBlog(@Path("id") id: String): ActionResponse

    // Blog approvals
    @GET("admin/blog-approvals") suspend fun blogApprovals(@Query("page") page: Int, @Query("q") q: String?): Paged<BlogRow>
    @GET("admin/blog-approvals/{id}") suspend fun blogApproval(@Path("id") id: String): BlogResponse
    @POST("admin/blog-approvals/{id}/approve") suspend fun approveBlog(@Path("id") id: String): ActionResponse
    @POST("admin/blog-approvals/{id}/reject") suspend fun rejectBlog(@Path("id") id: String): ActionResponse

    // Fatwa (admin)
    @GET("admin/fatwa") suspend fun fatwas(@Query("page") page: Int, @Query("q") q: String?, @Query("filter") filter: String?): Paged<FatwaRow>
    @GET("admin/fatwa/{id}") suspend fun fatwa(@Path("id") id: String): FatwaResponse
    @Multipart @POST("admin/fatwa/{id}/answer") suspend fun answerFatwa(@Path("id") id: String, @Part parts: List<MultipartBody.Part>): ActionResponse
    @Multipart @POST("admin/fatwa/{id}/reject") suspend fun rejectFatwa(@Path("id") id: String, @Part parts: List<MultipartBody.Part>): ActionResponse
    @DELETE("admin/fatwa/{id}") suspend fun deleteFatwa(@Path("id") id: String): ActionResponse

    // Digital library
    @GET("admin/library") suspend fun library(@Query("page") page: Int, @Query("q") q: String?): Paged<LibraryItemDto>
    @GET("admin/library?pageSize=1") suspend fun libraryOptions(): LibraryOptions
    @GET("admin/library/{id}") suspend fun libraryItem(@Path("id") id: String): LibraryResponse
    @Multipart @POST("admin/library") suspend fun createLibraryItem(@Part parts: List<MultipartBody.Part>): ActionResponse
    @Multipart @PATCH("admin/library/{id}") suspend fun updateLibraryItem(@Path("id") id: String, @Part parts: List<MultipartBody.Part>): ActionResponse
    @DELETE("admin/library/{id}") suspend fun deleteLibraryItem(@Path("id") id: String): ActionResponse

    // Verse & hadith of the day
    @GET("admin/daily-inspiration") suspend fun inspirations(@Query("page") page: Int, @Query("q") q: String?): Paged<InspirationDto>
    @GET("admin/daily-inspiration/{id}") suspend fun inspiration(@Path("id") id: String): InspirationResponse
    @Multipart @POST("admin/daily-inspiration") suspend fun createInspiration(@Part parts: List<MultipartBody.Part>): ActionResponse
    @Multipart @PATCH("admin/daily-inspiration/{id}") suspend fun updateInspiration(@Path("id") id: String, @Part parts: List<MultipartBody.Part>): ActionResponse
    @DELETE("admin/daily-inspiration/{id}") suspend fun deleteInspiration(@Path("id") id: String): ActionResponse

    // Teacher portal
    @GET("teacher/blogs") suspend fun teacherBlogs(@Query("page") page: Int): Paged<BlogRow>
    @GET("teacher/blogs?pageSize=1") suspend fun teacherBlogLimits(): ContentLimits
    @GET("teacher/blogs/{id}") suspend fun teacherBlog(@Path("id") id: String): BlogResponse
    @Multipart @POST("teacher/blogs") suspend fun createTeacherBlog(@Part parts: List<MultipartBody.Part>): ActionResponse
    @Multipart @PATCH("teacher/blogs/{id}") suspend fun updateTeacherBlog(@Path("id") id: String, @Part parts: List<MultipartBody.Part>): ActionResponse
    @DELETE("teacher/blogs/{id}") suspend fun deleteTeacherBlog(@Path("id") id: String): ActionResponse

    @GET("teacher/fatwa") suspend fun teacherFatwas(@Query("page") page: Int, @Query("q") q: String?, @Query("filter") filter: String?): Paged<FatwaRow>
    @GET("teacher/fatwa/{id}") suspend fun teacherFatwa(@Path("id") id: String): FatwaResponse
    @Multipart @POST("teacher/fatwa/{id}/answer") suspend fun answerTeacherFatwa(@Path("id") id: String, @Part parts: List<MultipartBody.Part>): ActionResponse
}

// ------------------------------------------------------------- status tones

/** Chip colours matching the website's badges. */
fun contentTone(label: String): Tone = when (label) {
    "Published", "Answered" -> Tone.Success
    "Home + Public", "On homepage", "Approved" -> Tone.Info
    "Pending approval", "Pending Review" -> Tone.Warning
    "Rejected" -> Tone.Danger
    else -> Tone.Neutral
}
