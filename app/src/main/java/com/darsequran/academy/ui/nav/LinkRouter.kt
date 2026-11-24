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
    val parts = path.trim('/').split('/').filter { it.isNotEmpty() }
    return when (parts.getOrNull(0)) {
        "admin" -> when {
            !shell.adminPanel -> null
            parts.size == 1 -> HrefDestination(AdminDashboardTab, isTab = true)
            else -> routeForAdminHref(path)?.let { HrefDestination(it, underTab = AdminDashboardTab) }
        }
        "teacher" -> if (shell == Shell.Teacher) HrefDestination(TeachTab, isTab = true) else null
        "profile" -> routeForProfileHref(parts.drop(1))
        else -> routeForPublicHref(parts)?.let(::HrefDestination)
    }
}

/** The website's public pages that have a screen in the app; "/fatwa/ask" is the form, not a fatwa called "ask". */
private fun routeForPublicHref(parts: List<String>): Any? {
    val id = parts.getOrNull(1) ?: return if (parts == listOf("bookstore")) BookstoreRoute else null
    return when (parts[0]) {
        "blog" -> BlogPostRoute(id)
        "fatwa" -> if (id == "ask") AskFatwaRoute else FatwaDetailRoute(id)
        "announcements" -> AnnouncementRoute(id)
        "courses" -> CourseRoute(id)
        "library" -> LibraryItemRoute(id)
        "teachers" -> TeacherRoute(id)
        "bookstore" -> if (id == "orders") OrdersRoute else BookRoute(id)
        else -> null
    }
}

/** The student's own pages ("/profile/..."); the profile itself is the Account tab. */
private fun routeForProfileHref(rest: List<String>): HrefDestination = when (rest.getOrNull(0)) {
    "courses" -> rest.getOrNull(1)?.let { HrefDestination(LearnRoute(it, "Course")) }
        ?: HrefDestination(CoursesTab, isTab = true)
    "payments" -> HrefDestination(PaymentsRoute)
    "cart" -> HrefDestination(OrdersRoute)
    "waiver-requests" -> HrefDestination(WaiversRoute)
    "reviews" -> HrefDestination(ReviewsRoute)
    "notifications" -> HrefDestination(NotificationsRoute)
    else -> HrefDestination(AccountTab, isTab = true)
}
