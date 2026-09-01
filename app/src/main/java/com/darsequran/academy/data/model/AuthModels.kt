package com.darsequran.academy.data.model

import com.google.gson.annotations.SerializedName

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
    @SerializedName("role") val role: String?,
    @SerializedName("image") val image: String?,
    @SerializedName("registrationNumber") val registrationNumber: String?,
    @SerializedName("emailVerified") val emailVerified: String?
)

data class AuthResponse(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("message") val message: String? = null,
    @SerializedName("error") val error: String? = null,
    @SerializedName("token") val token: String? = null,
    @SerializedName("user") val user: UserDto? = null
)
