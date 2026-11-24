package com.darsequran.academy.ui.account

private val ORIGIN = Regex("^https?://[^/]+")

/**
 * The website's label for where a notification leads (app/profile/notifications/[id]/page.tsx
 * relatedPageLabel); null when it leads nowhere but the notifications list.
 */
fun relatedPageLabel(href: String): String? {
    val path = href.substringBefore('?').replace(ORIGIN, "")
    return when {
        path.isEmpty() || path == "/profile/notifications" -> null
        path.startsWith("/profile/payments") -> "View in Payments"
        "/announcements" in path -> "View announcement"
        path.startsWith("/profile/courses") -> "View in My Courses"
        path.startsWith("/profile/cart") || path.startsWith("/bookstore") -> "View order"
        path.startsWith("/blog/") -> "View blog post"
        else -> "View related page"
    }
}
