package com.darsequran.academy.data.model

import com.google.gson.annotations.SerializedName

// Authentication DTOs
data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class RegisterRequest(
    @SerializedName("name") val name: String?,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("acceptPolicies") val acceptPolicies: Boolean = true
)

data class GoogleLoginRequest(
    @SerializedName("idToken") val idToken: String
)

data class UserDto(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("fatherName") val fatherName: String? = null,
    @SerializedName("dateOfBirth") val dateOfBirth: String? = null,
    @SerializedName("occupation") val occupation: String? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("whatsapp") val whatsapp: String? = null,
    @SerializedName("gender") val gender: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("image") val image: String? = null,
    @SerializedName("registrationNumber") val registrationNumber: String? = null,
    @SerializedName("emailVerified") val emailVerified: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null
)

data class AuthResponse(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("message") val message: String? = null,
    @SerializedName("error") val error: String? = null,
    @SerializedName("token") val token: String? = null,
    @SerializedName("user") val user: UserDto? = null
)

data class UserProfileResponse(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("error") val error: String? = null,
    @SerializedName("user") val user: UserDto? = null
)

// Daily Inspiration DTOs
data class InspirationDto(
    @SerializedName("arabicText") val arabicText: String?,
    @SerializedName("englishTranslation") val englishTranslation: String?,
    @SerializedName("reference") val reference: String?
)

data class DailyInspirationResponse(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("error") val error: String? = null,
    @SerializedName("inspiration") val inspiration: InspirationDto? = null
)

// Course & Enrollment DTOs
data class TeacherDto(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("email") val email: String? = null,
    @SerializedName("bio") val bio: String? = null
)

data class CourseDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("slug") val slug: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("fee") val fee: Double? = null,
    @SerializedName("registrationFee") val registrationFee: Double? = null,
    @SerializedName("teacher") val teacher: TeacherDto? = null
)

data class EnrollmentDto(
    @SerializedName("id") val id: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("courseId") val courseId: String,
    @SerializedName("status") val status: String,
    @SerializedName("rollNumber") val rollNumber: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("course") val course: CourseDto? = null
)

data class EnrollmentsResponse(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("error") val error: String? = null,
    @SerializedName("data") val data: List<EnrollmentDto>? = null
)

data class CoursesResponse(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("error") val error: String? = null,
    @SerializedName("data") val data: List<CourseDto>? = null,
    @SerializedName("totalCount") val totalCount: Int? = null,
    @SerializedName("page") val page: Int? = null,
    @SerializedName("pageSize") val pageSize: Int? = null
)
