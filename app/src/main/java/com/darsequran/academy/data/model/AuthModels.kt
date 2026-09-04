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

data class UpdateProfileRequest(
    @SerializedName("name") val name: String? = null,
    @SerializedName("fatherName") val fatherName: String? = null,
    @SerializedName("dateOfBirth") val dateOfBirth: String? = null,
    @SerializedName("occupation") val occupation: String? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("whatsapp") val whatsapp: String? = null,
    @SerializedName("gender") val gender: String? = null,
    @SerializedName("image") val image: String? = null
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
    @SerializedName("specialization") val specialization: String? = null,
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("initials") val initials: String? = null
)

data class CourseDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("slug") val slug: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("level") val level: String? = null,
    @SerializedName("startDate") val startDate: String? = null,
    @SerializedName("duration") val duration: String? = null,
    @SerializedName("fee") val fee: Double? = null,
    @SerializedName("billingCycle") val billingCycle: String? = null,
    @SerializedName("registrationFee") val registrationFee: Double? = null,
    @SerializedName("priceInrPaise") val priceInrPaise: Long? = null,
    @SerializedName("monthlyFeeInrPaise") val monthlyFeeInrPaise: Long? = null,
    @SerializedName("feeFrequency") val feeFrequency: String? = null,
    @SerializedName("teacher") val teacher: TeacherDto? = null,
    @SerializedName("syllabus") val syllabus: String? = null,
    @SerializedName("learningOutcomes") val learningOutcomes: String? = null
) {
    val displayEnrollmentFee: Int
        get() {
            if (registrationFee != null) return registrationFee.toInt()
            if (priceInrPaise != null) return (priceInrPaise / 100).toInt()
            if (fee != null) return fee.toInt()
            return 0
        }

    val displayMonthlyFee: Int
        get() {
            if (monthlyFeeInrPaise != null) return (monthlyFeeInrPaise / 100).toInt()
            if (fee != null) return fee.toInt()
            return 0
        }

    val displayFeeFrequency: String
        get() = billingCycle ?: feeFrequency?.lowercase()?.replace("_", " ")?.replaceFirstChar { it.uppercase() } ?: "Monthly"
}

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

data class EnrollCourseRequest(
    @SerializedName("courseId") val courseId: String
)

data class EnrollCourseResponse(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("alreadyEnrolled") val alreadyEnrolled: Boolean? = false,
    @SerializedName("error") val error: String? = null,
    @SerializedName("enrollment") val enrollment: EnrollmentDto? = null
)

data class CoursesResponse(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("error") val error: String? = null,
    @SerializedName("data") val data: List<CourseDto>? = null,
    @SerializedName("totalCount") val totalCount: Int? = null,
    @SerializedName("page") val page: Int? = null,
    @SerializedName("pageSize") val pageSize: Int? = null
)

data class SingleCourseResponse(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("error") val error: String? = null,
    @SerializedName("course") val course: CourseDto? = null
)
