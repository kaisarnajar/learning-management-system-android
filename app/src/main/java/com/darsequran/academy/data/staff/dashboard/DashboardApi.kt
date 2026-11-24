package com.darsequran.academy.data.staff.dashboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.serialization.Serializable
import retrofit2.http.GET

@Serializable data class DashboardLink(val href: String, val label: String, val count: Int? = null, val highlight: Boolean = false)
@Serializable data class DashboardGroup(val title: String, val links: List<DashboardLink> = emptyList())
@Serializable data class DashboardResponse(val groups: List<DashboardGroup> = emptyList(), val actionRequired: List<DashboardLink> = emptyList())

/**
 * The website's admin dashboard rules (app/admin/page.tsx): tool pages in [quickLinkHrefs] are "Manage" cards with no
 * number; other pages show their count, and a highlighted one only under Action Required, not in its group too. A page
 * with neither a count nor a place among the tool pages is not on the dashboard.
 */
object DashboardLayout {
    /** The website's ADMIN_DASHBOARD_LINK_HREFS. */
    val quickLinkHrefs = setOf(
        "/admin/payment-settings", "/admin/finance", "/admin/transactions",
        "/admin/coupons/finance", "/admin/audit-log",
    )

    fun isQuickLink(link: DashboardLink) = link.href in quickLinkHrefs

    fun actionRequired(data: DashboardResponse): List<DashboardLink> =
        data.groups.flatMap { it.links }.filter { !isQuickLink(it) && it.count != null && it.highlight }

    /** Each group with the cards it shows; groups left empty are dropped. */
    fun groups(data: DashboardResponse): List<DashboardGroup> = data.groups
        .map { g -> g.copy(links = g.links.filter { isQuickLink(it) || (it.count != null && !it.highlight) }) }
        .filter { it.links.isNotEmpty() }
}

interface DashboardApi {
    @GET("admin/dashboard") suspend fun dashboard(): DashboardResponse
}

// --------------------------------------------------------------- screens
