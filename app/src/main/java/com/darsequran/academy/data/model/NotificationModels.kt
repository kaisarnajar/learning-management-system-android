package com.darsequran.academy.data.model

import com.google.gson.annotations.SerializedName

data class NotificationDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("message") val message: String,
    @SerializedName("type") val type: String? = "ANNOUNCEMENT",
    @SerializedName("isRead") val isRead: Boolean = false,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("courseId") val courseId: String? = null
)

data class NotificationsResponse(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("error") val error: String? = null,
    @SerializedName("data") val data: List<NotificationDto>? = null,
    @SerializedName("totalCount") val totalCount: Int? = 0,
    @SerializedName("unreadCount") val unreadCount: Int? = 0,
    @SerializedName("page") val page: Int? = 1,
    @SerializedName("pageSize") val pageSize: Int? = 20
)

data class MarkReadRequest(
    @SerializedName("notificationId") val notificationId: String? = null,
    @SerializedName("markAllRead") val markAllRead: Boolean? = null
)

data class DeviceTokenRequest(
    @SerializedName("token") val token: String,
    @SerializedName("platform") val platform: String = "android"
)
