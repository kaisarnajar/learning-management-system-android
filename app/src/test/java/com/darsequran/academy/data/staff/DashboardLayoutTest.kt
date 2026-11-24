package com.darsequran.academy.data.staff

import com.darsequran.academy.data.staff.dashboard.DashboardGroup
import com.darsequran.academy.data.staff.dashboard.DashboardLayout
import com.darsequran.academy.data.staff.dashboard.DashboardLink
import com.darsequran.academy.data.staff.dashboard.DashboardResponse
import org.junit.Assert.assertEquals
import org.junit.Test

/** The admin dashboard shows what the website's dashboard shows. */
class DashboardLayoutTest {
    private val data = DashboardResponse(
        groups = listOf(
            DashboardGroup(
                "Payments & Finance",
                listOf(
                    DashboardLink("/admin/payments", "Pending Payments", count = 3, highlight = true),
                    DashboardLink("/admin/receipts", "Payment Receipts", count = 12),
                    // The API sends 0 for finance; the website shows it as a Manage card.
                    DashboardLink("/admin/finance", "Finance", count = 0),
                    DashboardLink("/admin/shipping-charges", "Shipping charges"),
                ),
            ),
            DashboardGroup("System Settings", listOf(DashboardLink("/admin/audit-log", "Audit log"))),
        ),
    )

    @Test
    fun aHighlightedPageIsOnlyUnderActionRequired() {
        assertEquals(listOf("/admin/payments"), DashboardLayout.actionRequired(data).map { it.href })
        assertEquals(listOf("/admin/receipts", "/admin/finance"), DashboardLayout.groups(data)[0].links.map { it.href })
    }

    @Test
    fun toolPagesAreManageCardsAndPagesWithoutACountAreLeftOut() {
        val links = DashboardLayout.groups(data).flatMap { it.links }
        assertEquals(false, links.any { it.href == "/admin/shipping-charges" })
        assertEquals(true, DashboardLayout.isQuickLink(links.first { it.href == "/admin/finance" }))
        assertEquals(listOf("Payments & Finance", "System Settings"), DashboardLayout.groups(data).map { it.title })
    }
}
