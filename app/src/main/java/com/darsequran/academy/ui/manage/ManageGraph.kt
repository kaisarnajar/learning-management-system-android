package com.darsequran.academy.ui.manage

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.darsequran.academy.ui.manage.people.AuditLogRoute
import com.darsequran.academy.ui.manage.people.CertificatesAdminRoute
import com.darsequran.academy.ui.manage.people.EnrollmentsAdminRoute
import com.darsequran.academy.ui.manage.people.InquiriesAdminRoute
import com.darsequran.academy.ui.manage.people.ReviewsAdminRoute
import com.darsequran.academy.ui.manage.people.TeachersAdminRoute
import com.darsequran.academy.ui.manage.people.peopleGraph
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
    peopleGraph(nav)
}

/** Website admin page → the app screen that does the same job. */
val adminHrefRoutes: Map<String, Any> = mapOf(
    "/admin/students" to StudentsAdminRoute,
    "/admin/teachers" to TeachersAdminRoute,
    "/admin/enrollments" to EnrollmentsAdminRoute,
    "/admin/certificates" to CertificatesAdminRoute,
    "/admin/review-approvals" to ReviewsAdminRoute,
    "/admin/contact-inquiries" to InquiriesAdminRoute,
    "/admin/audit-log" to AuditLogRoute,
)

/** The app screen for a website admin path, or null when the app has none. */
fun routeForAdminHref(href: String): Any? = adminHrefRoutes[href.substringBefore('?').trimEnd('/')]
