package com.darsequran.academy.ui.manage.people

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.darsequran.academy.ui.manage.routeForAdminHref
import com.darsequran.academy.ui.manage.students.StudentAdminRoute
import kotlinx.serialization.Serializable

// Teachers
@Serializable data object TeachersAdminRoute
@Serializable data class TeacherAdminRoute(val id: String)
@Serializable data class TeacherFormRoute(val id: String? = null)

// Enrollments
@Serializable data object EnrollmentsAdminRoute
@Serializable data object EnrollStudentRoute

// Certificates
@Serializable data object CertificatesAdminRoute
@Serializable data class CertificateAdminRoute(val enrollmentId: String)
@Serializable data object GenerateCertificateRoute

// Reviews
@Serializable data object ReviewsAdminRoute
@Serializable data class ReviewAdminRoute(val id: String)
@Serializable data class ReviewEditRoute(val id: String)

// Contact inquiries
@Serializable data object InquiriesAdminRoute
@Serializable data class InquiryAdminRoute(val id: String)
@Serializable data class InquiryReplyRoute(val id: String)

// Audit log
@Serializable data object AuditLogRoute

fun NavGraphBuilder.peopleGraph(nav: NavHostController) {
    composable<TeachersAdminRoute> {
        TeachersListScreen(nav::popBackStack, onOpen = { nav.navigate(TeacherAdminRoute(it)) }, onAdd = { nav.navigate(TeacherFormRoute()) })
    }
    composable<TeacherAdminRoute> {
        val id = it.toRoute<TeacherAdminRoute>().id
        TeacherDetailScreen(id, nav::popBackStack, onEdit = { nav.navigate(TeacherFormRoute(id)) })
    }
    composable<TeacherFormRoute> { TeacherFormScreen(it.toRoute<TeacherFormRoute>().id, nav::popBackStack) }

    composable<EnrollmentsAdminRoute> {
        val payments = routeForAdminHref("/admin/payments")
        EnrollmentsScreen(
            nav::popBackStack,
            onStudent = { id -> nav.navigate(StudentAdminRoute(id)) },
            onEnroll = { nav.navigate(EnrollStudentRoute) },
            onPayments = payments?.let { route -> { nav.navigate(route) } },
        )
    }
    composable<EnrollStudentRoute> { EnrollStudentScreen(nav::popBackStack) }

    composable<CertificatesAdminRoute> {
        CertificatesListScreen(nav::popBackStack, onOpen = { nav.navigate(CertificateAdminRoute(it)) }, onGenerate = { nav.navigate(GenerateCertificateRoute) })
    }
    composable<CertificateAdminRoute> { CertificateDetailScreen(it.toRoute<CertificateAdminRoute>().enrollmentId, nav::popBackStack) }
    composable<GenerateCertificateRoute> {
        GenerateCertificateScreen(nav::popBackStack, onGenerated = { id ->
            nav.navigate(CertificateAdminRoute(id)) { popUpTo(GenerateCertificateRoute) { inclusive = true } }
        })
    }

    composable<ReviewsAdminRoute> {
        ReviewsListScreen(nav::popBackStack, onOpen = { nav.navigate(ReviewAdminRoute(it)) }, onEdit = { nav.navigate(ReviewEditRoute(it)) })
    }
    composable<ReviewAdminRoute> {
        val id = it.toRoute<ReviewAdminRoute>().id
        ReviewDetailScreen(id, nav::popBackStack, onEdit = { nav.navigate(ReviewEditRoute(id)) })
    }
    composable<ReviewEditRoute> { ReviewEditScreen(it.toRoute<ReviewEditRoute>().id, nav::popBackStack) }

    composable<InquiriesAdminRoute> {
        InquiriesListScreen(nav::popBackStack, onOpen = { nav.navigate(InquiryAdminRoute(it)) }, onReply = { nav.navigate(InquiryReplyRoute(it)) })
    }
    composable<InquiryAdminRoute> {
        val id = it.toRoute<InquiryAdminRoute>().id
        InquiryDetailScreen(id, nav::popBackStack, onReply = { nav.navigate(InquiryReplyRoute(id)) })
    }
    composable<InquiryReplyRoute> { InquiryReplyScreen(it.toRoute<InquiryReplyRoute>().id, nav::popBackStack) }

    composable<AuditLogRoute> { AuditLogScreen(nav::popBackStack) }
}
