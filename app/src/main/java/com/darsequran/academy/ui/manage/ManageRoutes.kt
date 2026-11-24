package com.darsequran.academy.ui.manage

import kotlinx.serialization.Serializable

// Admin tabs (admins and developers). Each hub lists some of the website's admin sidebar groups.
@Serializable data object AdminDashboardTab
@Serializable data object AdminAcademicsTab
@Serializable data object AdminFinanceTab
@Serializable data object AdminContentTab

// Teacher tab (teachers keep the student tabs and get this one too).
@Serializable data object TeachTab

// Developer
@Serializable data object EnvironmentRoute

/** Which website sidebar groups each admin hub tab shows (titles as /api/v1/admin/dashboard returns them). */
object AdminHubs {
    val academics = listOf("User Management", "Academic Management")
    val finance = listOf("Payments & Finance", "Fee Waivers", "Bookstore")
    val content = listOf("Content Management", "System Settings")
}
