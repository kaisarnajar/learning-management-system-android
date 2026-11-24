package com.darsequran.academy.ui.manage

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.darsequran.academy.ui.manage.content.contentGraph
import com.darsequran.academy.ui.manage.content.AnnouncementsAdminRoute
import com.darsequran.academy.ui.manage.content.BlogApprovalsRoute
import com.darsequran.academy.ui.manage.content.BlogsAdminRoute
import com.darsequran.academy.ui.manage.content.FatwaAdminRoute
import com.darsequran.academy.ui.manage.content.InspirationAdminRoute
import com.darsequran.academy.ui.manage.content.LibraryAdminRoute
import com.darsequran.academy.ui.manage.students.StudentsAdminRoute
import com.darsequran.academy.ui.manage.students.studentsAdminGraph
import com.darsequran.academy.ui.teach.teachGraph

/*
 * Registry of the admin and teacher sections.
 *
 * A section lives in its own package (ui/manage/<section>, ui/teach) with its API, screens and a
 * `NavGraphBuilder.<section>Graph(nav)` function. To add one:
 *   1. call its graph function in [manageGraph];
 *   2. map the website's admin path(s) it covers to its first screen in [adminHrefRoutes], so the dashboard and
 *      the hub tabs open it.
 */

/** All admin and teacher screens. */
fun NavGraphBuilder.manageGraph(nav: NavHostController) {
    studentsAdminGraph(nav)
    teachGraph(nav)
    contentGraph(nav)
}

/** Website admin page → the app screen that does the same job. */
val adminHrefRoutes: Map<String, Any> = mapOf(
    "/admin/students" to StudentsAdminRoute,
    "/admin/announcements" to AnnouncementsAdminRoute,
    "/admin/blogs" to BlogsAdminRoute,
    "/admin/blog-approvals" to BlogApprovalsRoute,
    "/admin/fatwa" to FatwaAdminRoute,
    "/admin/library" to LibraryAdminRoute,
    "/admin/daily-inspiration" to InspirationAdminRoute,
)

/** The app screen for a website admin path, or null when the app has none. */
fun routeForAdminHref(href: String): Any? = adminHrefRoutes[href.substringBefore('?').trimEnd('/')]
