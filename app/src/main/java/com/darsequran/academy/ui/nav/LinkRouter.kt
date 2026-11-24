package com.darsequran.academy.ui.nav

import com.darsequran.academy.ui.manage.AdminDashboardTab
import com.darsequran.academy.ui.manage.TeachTab
import com.darsequran.academy.ui.manage.routeForAdminHref

/**
 * Where a website link leads in the app: a screen to open, or a tab to switch to ([isTab]). A screen with [underTab]
 * opens on top of that tab (staff pages open inside the staff tab).
 */
data class HrefDestination(val route: Any, val isTab: Boolean = false, val underTab: Any? = null)

/**
 * The app's screen for a path of the academy's website (from a notification or a link in an article), for a user in
 * [shell]; null when the app has no screen for it. Staff pages only lead somewhere for staff: admin pages for admins,
 * the teaching tab for teachers.
 */
internal fun routeForHref(href: String, shell: Shell): HrefDestination? {
    val path = href.substringBefore('#').substringBefore('?')
    val parts = path.trim('/').split('/')
    fun go(route: Any) = HrefDestination(route)
    return when {
        parts.getOrNull(0) == "admin" -> when {
            !shell.adminPanel -> null
            parts.size == 1 -> HrefDestination(AdminDashboardTab, isTab = true)
            else -> routeForAdminHref(path)?.let { HrefDestination(it, underTab = AdminDashboardTab) }
        }
        parts.getOrNull(0) == "teacher" -> if (shell == Shell.Teacher) HrefDestination(TeachTab, isTab = true) else null
        parts.getOrNull(0) == "blog" && parts.size >= 2 -> go(BlogPostRoute(parts[1]))
        parts.getOrNull(0) == "fatwa" && parts.size >= 2 -> go(FatwaDetailRoute(parts[1]))
        parts.getOrNull(0) == "announcements" && parts.size >= 2 -> go(AnnouncementRoute(parts[1]))
        parts.getOrNull(0) == "courses" && parts.size >= 2 -> go(CourseRoute(parts[1]))
        parts.take(2) == listOf("profile", "courses") && parts.size >= 3 -> go(LearnRoute(parts[2], "Course"))
        parts.take(2) == listOf("profile", "payments") -> go(PaymentsRoute)
        parts.take(2) == listOf("profile", "cart") || parts.getOrNull(0) == "bookstore" -> go(OrdersRoute)
        parts.take(2) == listOf("profile", "waiver-requests") -> go(WaiversRoute)
        parts.getOrNull(0) == "profile" -> HrefDestination(CoursesTab, isTab = true)
        else -> null
    }
}
