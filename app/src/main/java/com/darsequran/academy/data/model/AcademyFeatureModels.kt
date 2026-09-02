package com.darsequran.academy.data.model

import com.google.gson.annotations.SerializedName

// ==========================================
// 1. Digital Library DTOs
// ==========================================
data class LibraryBookDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("author") val author: String? = null,
    @SerializedName("topic") val topic: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("fileUrl") val fileUrl: String? = null,
    @SerializedName("filePath") val filePath: String? = null,
    @SerializedName("coverImagePath") val coverImagePath: String? = null,
    @SerializedName("pages") val pages: Int? = null,
    @SerializedName("published") val published: Boolean = true
)

data class LibraryResponse(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("error") val error: String? = null,
    @SerializedName("data") val data: List<LibraryBookDto>? = emptyList(),
    @SerializedName("totalCount") val totalCount: Int? = 0,
    @SerializedName("page") val page: Int? = 1,
    @SerializedName("pageSize") val pageSize: Int? = 20
)

// ==========================================
// 2. Announcements DTOs
// ==========================================
data class AnnouncementImageDto(
    @SerializedName("id") val id: String,
    @SerializedName("imagePath") val imagePath: String,
    @SerializedName("caption") val caption: String? = null
)

data class AnnouncementAuthorDto(
    @SerializedName("name") val name: String? = null
)

data class AnnouncementDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("body") val body: String? = null,
    @SerializedName("location") val location: String? = null,
    @SerializedName("tag") val tag: String? = null,
    @SerializedName("priority") val priority: String? = "Normal",
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("images") val images: List<AnnouncementImageDto>? = emptyList(),
    @SerializedName("createdBy") val createdBy: AnnouncementAuthorDto? = null
)

data class AnnouncementsResponse(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("error") val error: String? = null,
    @SerializedName("data") val data: List<AnnouncementDto>? = emptyList(),
    @SerializedName("totalCount") val totalCount: Int? = 0,
    @SerializedName("page") val page: Int? = 1,
    @SerializedName("pageSize") val pageSize: Int? = 20
)

// ==========================================
// 3. Blog DTOs
// ==========================================
data class BlogImageDto(
    @SerializedName("id") val id: String,
    @SerializedName("imagePath") val imagePath: String,
    @SerializedName("caption") val caption: String? = null
)

data class BlogAuthorDto(
    @SerializedName("name") val name: String? = null,
    @SerializedName("email") val email: String? = null
)

data class BlogPostDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("excerpt") val excerpt: String? = null,
    @SerializedName("body") val body: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("readTime") val readTime: String? = "5 min read",
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("images") val images: List<BlogImageDto>? = emptyList(),
    @SerializedName("createdBy") val createdBy: BlogAuthorDto? = null
)

data class BlogResponse(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("error") val error: String? = null,
    @SerializedName("data") val data: List<BlogPostDto>? = emptyList(),
    @SerializedName("totalCount") val totalCount: Int? = 0,
    @SerializedName("page") val page: Int? = 1,
    @SerializedName("pageSize") val pageSize: Int? = 20
)

// ==========================================
// 4. Teachers DTOs
// ==========================================
data class TeacherProfileDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String? = null,
    @SerializedName("specialization") val specialization: String? = null,
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("initials") val initials: String? = null,
    @SerializedName("imageUrl") val imageUrl: String? = null,
    @SerializedName("published") val published: Boolean = true
)

data class TeachersResponse(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("error") val error: String? = null,
    @SerializedName("data") val data: List<TeacherProfileDto>? = emptyList(),
    @SerializedName("totalCount") val totalCount: Int? = 0,
    @SerializedName("page") val page: Int? = 1,
    @SerializedName("pageSize") val pageSize: Int? = 20
)

// ==========================================
// 5. Fatwa & Fiqh Q&A DTOs
// ==========================================
data class FatwaItemDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("question") val question: String,
    @SerializedName("answer") val answer: String? = null,
    @SerializedName("category") val category: String = "General",
    @SerializedName("askerName") val askerName: String? = null,
    @SerializedName("scholarName") val scholarName: String? = null,
    @SerializedName("answeredAt") val answeredAt: String? = null,
    @SerializedName("approvalStatus") val approvalStatus: String? = "APPROVED"
)

data class SubmitFatwaRequest(
    @SerializedName("title") val title: String,
    @SerializedName("question") val question: String,
    @SerializedName("category") val category: String,
    @SerializedName("askerName") val askerName: String,
    @SerializedName("askerEmail") val askerEmail: String
)

data class FatwaResponse(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("error") val error: String? = null,
    @SerializedName("data") val data: List<FatwaItemDto>? = emptyList(),
    @SerializedName("totalCount") val totalCount: Int? = 0,
    @SerializedName("page") val page: Int? = 1,
    @SerializedName("pageSize") val pageSize: Int? = 20
)

// ==========================================
// 6. Bookstore DTOs
// ==========================================
data class BookstoreItemDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("author") val author: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("priceInrPaise") val priceInrPaise: Long = 0,
    @SerializedName("mrpInrPaise") val mrpInrPaise: Long? = 0,
    @SerializedName("status") val status: String? = "AVAILABLE",
    @SerializedName("imagePath") val imagePath: String? = null,
    @SerializedName("category") val category: String? = "Islamic Books"
) {
    val priceInRupees: Double get() = priceInrPaise / 100.0
}

data class BookstoreResponse(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("error") val error: String? = null,
    @SerializedName("data") val data: List<BookstoreItemDto>? = emptyList(),
    @SerializedName("totalCount") val totalCount: Int? = 0,
    @SerializedName("page") val page: Int? = 1,
    @SerializedName("pageSize") val pageSize: Int? = 20
)
