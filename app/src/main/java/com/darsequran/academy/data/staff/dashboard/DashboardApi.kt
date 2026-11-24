package com.darsequran.academy.data.staff.dashboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.serialization.Serializable
import retrofit2.http.GET

@Serializable data class DashboardLink(val href: String, val label: String, val count: Int? = null, val highlight: Boolean = false)
@Serializable data class DashboardGroup(val title: String, val links: List<DashboardLink> = emptyList())
@Serializable data class DashboardResponse(val groups: List<DashboardGroup> = emptyList(), val actionRequired: List<DashboardLink> = emptyList())

interface DashboardApi {
    @GET("admin/dashboard") suspend fun dashboard(): DashboardResponse
}

// --------------------------------------------------------------- screens
