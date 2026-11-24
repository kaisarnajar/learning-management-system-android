package com.darsequran.academy.ui.manage

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.darsequran.academy.data.staff.courses.Portal
import com.darsequran.academy.ui.manage.bookstore.BookAdminRoute
import com.darsequran.academy.ui.manage.bookstore.BookOrderAdminRoute
import com.darsequran.academy.ui.manage.content.BlogApprovalRoute
import com.darsequran.academy.ui.manage.content.FatwaAdminDetailRoute
import com.darsequran.academy.ui.manage.content.InspirationDetailRoute
import com.darsequran.academy.ui.manage.content.LibraryAdminDetailRoute
import com.darsequran.academy.ui.manage.courses.AttendanceListRoute
import com.darsequran.academy.ui.manage.courses.CourseAnnouncementsRoute
import com.darsequran.academy.ui.manage.courses.CourseHubRoute
import com.darsequran.academy.ui.manage.courses.CourseRosterRoute
import com.darsequran.academy.ui.manage.courses.GradeListRoute
import com.darsequran.academy.ui.manage.money.ReceiptRoute
import com.darsequran.academy.ui.manage.people.CertificateAdminRoute
import com.darsequran.academy.ui.manage.people.InquiryAdminRoute
import com.darsequran.academy.ui.manage.people.ReviewAdminRoute
import com.darsequran.academy.ui.manage.people.TeacherAdminRoute
import com.darsequran.academy.ui.manage.students.StudentAdminRoute
import com.darsequran.academy.ui.manage.bookstore.BookOrdersAdminRoute
import com.darsequran.academy.ui.manage.bookstore.BooksAdminRoute
import com.darsequran.academy.ui.manage.bookstore.bookstoreAdminGraph
import com.darsequran.academy.ui.manage.people.AuditLogRoute
import com.darsequran.academy.ui.manage.people.CertificatesAdminRoute
import com.darsequran.academy.ui.manage.people.EnrollmentsAdminRoute
import com.darsequran.academy.ui.manage.people.InquiriesAdminRoute
import com.darsequran.academy.ui.manage.people.ReviewsAdminRoute
import com.darsequran.academy.ui.manage.people.TeachersAdminRoute
import com.darsequran.academy.ui.manage.people.peopleGraph
import com.darsequran.academy.ui.manage.courses.CoursesAdminRoute
import com.darsequran.academy.ui.manage.courses.coursesGraph
import com.darsequran.academy.ui.manage.content.contentGraph
import com.darsequran.academy.ui.manage.content.AnnouncementsAdminRoute
import com.darsequran.academy.ui.manage.content.BlogApprovalsRoute
import com.darsequran.academy.ui.manage.content.BlogsAdminRoute
import com.darsequran.academy.ui.manage.content.FatwaAdminRoute
import com.darsequran.academy.ui.manage.content.InspirationAdminRoute
import com.darsequran.academy.ui.manage.content.LibraryAdminRoute
import com.darsequran.academy.ui.manage.money.BookstoreFinanceRoute
import com.darsequran.academy.ui.manage.money.CompletedPaymentsRoute
import com.darsequran.academy.ui.manage.money.CouponsRoute
import com.darsequran.academy.ui.manage.money.FinanceRoute
import com.darsequran.academy.ui.manage.money.PaymentSettingsRoute
import com.darsequran.academy.ui.manage.money.PendingPaymentsRoute
import com.darsequran.academy.ui.manage.money.ReceiptsRoute
import com.darsequran.academy.ui.manage.money.ShippingChargesRoute
import com.darsequran.academy.ui.manage.money.TransactionsRoute
import com.darsequran.academy.ui.manage.money.WaiverFinanceRoute
import com.darsequran.academy.ui.manage.money.moneyGraph
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
    bookstoreAdminGraph(nav)
    peopleGraph(nav)
    coursesGraph(nav)
    contentGraph(nav)
    moneyGraph(nav)
}

/** Website admin page → the app screen that does the same job. */
val adminHrefRoutes: Map<String, Any> = mapOf(
    "/admin/students" to StudentsAdminRoute,
    "/admin/bookstore" to BooksAdminRoute,
    "/admin/bookstore/orders" to BookOrdersAdminRoute,
    "/admin/teachers" to TeachersAdminRoute,
    "/admin/enrollments" to EnrollmentsAdminRoute,
    "/admin/certificates" to CertificatesAdminRoute,
    "/admin/review-approvals" to ReviewsAdminRoute,
    "/admin/contact-inquiries" to InquiriesAdminRoute,
    "/admin/audit-log" to AuditLogRoute,
    "/admin/courses" to CoursesAdminRoute,
    "/admin/announcements" to AnnouncementsAdminRoute,
    "/admin/blogs" to BlogsAdminRoute,
    "/admin/blog-approvals" to BlogApprovalsRoute,
    "/admin/fatwa" to FatwaAdminRoute,
    "/admin/library" to LibraryAdminRoute,
    "/admin/daily-inspiration" to InspirationAdminRoute,
    "/admin/payments" to PendingPaymentsRoute,
    "/admin/completed-payments" to CompletedPaymentsRoute,
    "/admin/receipts" to ReceiptsRoute,
    "/admin/finance" to FinanceRoute,
    "/admin/finance/bookstore" to BookstoreFinanceRoute,
    "/admin/transactions" to TransactionsRoute(),
    "/admin/payment-settings" to PaymentSettingsRoute,
    "/admin/shipping-charges" to ShippingChargesRoute,
    "/admin/coupons" to CouponsRoute,
    "/admin/coupons/finance" to WaiverFinanceRoute,
)

/** The app screen for a website admin path (a section, or one record's page), or null when the app has none. */
fun routeForAdminHref(href: String): Any? {
    val path = href.substringBefore('#').substringBefore('?').trimEnd('/')
    return adminHrefRoutes[path] ?: adminRecordRoute(path.removePrefix("/admin/").split('/'))
}

/** A record's own page on the website (`/admin/students/{id}`, …) → its app screen. */
private val adminRecordPages: Map<String, (String) -> Any> = mapOf(
    "students" to ::StudentAdminRoute,
    "teachers" to ::TeacherAdminRoute,
    "bookstore" to ::BookAdminRoute,
    "certificates" to ::CertificateAdminRoute,
    "contact-inquiries" to ::InquiryAdminRoute,
    "review-approvals" to ::ReviewAdminRoute,
    "blog-approvals" to ::BlogApprovalRoute,
    "fatwa" to ::FatwaAdminDetailRoute,
    "library" to ::LibraryAdminDetailRoute,
    "daily-inspiration" to ::InspirationDetailRoute,
    "receipts" to ::ReceiptRoute,
    "courses" to { id -> CourseHubRoute(Portal.Admin.path, id) },
)

/** A course's admin pages (`/admin/courses/{id}/grades`, …) → their app screens. */
private val adminCoursePages: Map<String, (String) -> Any> = mapOf(
    "students" to ::CourseRosterRoute,
    "attendance" to { id -> AttendanceListRoute(Portal.Admin.path, id) },
    "grades" to { id -> GradeListRoute(Portal.Admin.path, id) },
    "announcements" to { id -> CourseAnnouncementsRoute(Portal.Admin.path, id) },
)

/** One record's page (`p` is the path after /admin/, split): its screen, or null (forms such as `/edit` have none). */
private fun adminRecordRoute(p: List<String>): Any? {
    val id = p.getOrNull(1)?.takeIf { it.isNotBlank() && it != "new" } ?: return null
    return when {
        p.size == 2 -> adminRecordPages[p[0]]?.invoke(id)
        p[0] == "bookstore" && p[1] == "orders" && p.size == RECORD_UNDER_SECTION -> BookOrderAdminRoute(p[2])
        p[0] == "courses" && p.size == RECORD_UNDER_SECTION -> adminCoursePages[p[2]]?.invoke(id)
        else -> null
    }
}

/** `section/{id}/page` or `section/sub/{id}`. */
private const val RECORD_UNDER_SECTION = 3
