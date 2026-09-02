package com.darsequran.academy.data.model

import com.google.gson.annotations.SerializedName

data class StudentReviewDto(
    @SerializedName("id") val id: String,
    @SerializedName("userId") val userId: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("rating") val rating: Int = 5,
    @SerializedName("quote") val quote: String,
    @SerializedName("course") val course: String? = null,
    @SerializedName("location") val location: String? = null,
    @SerializedName("status") val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    @SerializedName("createdAt") val createdAt: String? = null
)

data class StudentReviewsResponse(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("error") val error: String? = null,
    @SerializedName("data") val data: List<StudentReviewDto>? = emptyList()
)

data class CreateReviewRequest(
    @SerializedName("rating") val rating: Int = 5,
    @SerializedName("quote") val quote: String,
    @SerializedName("course") val course: String? = null,
    @SerializedName("location") val location: String? = null
)

data class CartItemDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("type") val type: String = "COURSE",
    @SerializedName("price") val price: Double = 0.0
)
