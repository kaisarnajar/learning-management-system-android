package com.darsequran.academy.ui.manage.courses

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.darsequran.academy.ui.teach.TeacherStudentsRoute
import kotlinx.serialization.Serializable

/*
 * Routes of the admin course pages and of the course tools shared with the teacher portal. A shared route carries
 * `portal` ("admin" or "teacher", see [Portal]) and is registered once, here.
 */

// admin only (website: /admin/courses, /new, /[id]/edit, /[id]/students)
@Serializable data object CoursesAdminRoute
@Serializable data class CourseFormRoute(val id: String? = null)
@Serializable data class CourseRosterRoute(val courseId: String)

// shared (website: /admin/courses/[id]/… and /teacher/courses/[id]/…)
@Serializable data class CourseHubRoute(val portal: String, val courseId: String)
@Serializable data class CourseAnnouncementsRoute(val portal: String, val courseId: String, val enrollmentId: String? = null, val studentName: String? = null)
@Serializable data class AnnouncementFormRoute(val portal: String, val courseId: String, val enrollmentId: String? = null, val announcementId: String? = null, val studentName: String? = null)
@Serializable data class AttendanceListRoute(val portal: String, val courseId: String)
@Serializable data class AttendanceTakeRoute(val portal: String, val courseId: String, val date: String? = null)
@Serializable data class AttendanceDayRoute(val portal: String, val courseId: String, val date: String)
@Serializable data class StudentAttendanceRoute(val portal: String, val courseId: String, val enrollmentId: String)
@Serializable data class GradeListRoute(val portal: String, val courseId: String)
@Serializable data class GradeFormRoute(val portal: String, val courseId: String, val gradeId: String? = null)
@Serializable data class StudentGradesRoute(val portal: String, val courseId: String, val enrollmentId: String)

fun NavGraphBuilder.coursesGraph(nav: NavHostController) {
    composable<CoursesAdminRoute> {
        CoursesAdminListScreen(
            onBack = nav::popBackStack,
            onOpen = { nav.navigate(CourseHubRoute(Portal.Admin.path, it)) },
            onAdd = { nav.navigate(CourseFormRoute()) },
        )
    }
    composable<CourseFormRoute> { CourseFormScreen(it.toRoute<CourseFormRoute>().id, nav::popBackStack) }
    composable<CourseRosterRoute> {
        val r = it.toRoute<CourseRosterRoute>()
        CourseRosterScreen(
            courseId = r.courseId,
            onBack = nav::popBackStack,
            onAttendance = { e -> nav.navigate(StudentAttendanceRoute(Portal.Admin.path, r.courseId, e)) },
            onGrades = { e -> nav.navigate(StudentGradesRoute(Portal.Admin.path, r.courseId, e)) },
        )
    }

    composable<CourseHubRoute> {
        val r = it.toRoute<CourseHubRoute>()
        val portal = Portal.of(r.portal)
        CourseHubScreen(
            portal = portal,
            courseId = r.courseId,
            onBack = nav::popBackStack,
            onStudents = {
                if (portal == Portal.Admin) nav.navigate(CourseRosterRoute(r.courseId)) else nav.navigate(TeacherStudentsRoute(r.courseId))
            },
            onAttendance = { nav.navigate(AttendanceListRoute(r.portal, r.courseId)) },
            onGrades = { nav.navigate(GradeListRoute(r.portal, r.courseId)) },
            onAnnouncements = { nav.navigate(CourseAnnouncementsRoute(r.portal, r.courseId)) },
            onEdit = { nav.navigate(CourseFormRoute(r.courseId)) },
        )
    }

    composable<CourseAnnouncementsRoute> {
        val r = it.toRoute<CourseAnnouncementsRoute>()
        CourseAnnouncementsScreen(
            portal = Portal.of(r.portal), courseId = r.courseId, enrollmentId = r.enrollmentId, studentName = r.studentName,
            onBack = nav::popBackStack,
            onNew = { nav.navigate(AnnouncementFormRoute(r.portal, r.courseId, r.enrollmentId, null, r.studentName)) },
            onEdit = { a -> nav.navigate(AnnouncementFormRoute(r.portal, r.courseId, r.enrollmentId, a, r.studentName)) },
        )
    }
    composable<AnnouncementFormRoute> {
        val r = it.toRoute<AnnouncementFormRoute>()
        AnnouncementFormScreen(Portal.of(r.portal), r.courseId, r.enrollmentId, r.announcementId, r.studentName, nav::popBackStack)
    }

    composable<AttendanceListRoute> {
        val r = it.toRoute<AttendanceListRoute>()
        AttendanceListScreen(
            portal = Portal.of(r.portal), courseId = r.courseId, onBack = nav::popBackStack,
            onTake = { nav.navigate(AttendanceTakeRoute(r.portal, r.courseId)) },
            onOpen = { d -> nav.navigate(AttendanceDayRoute(r.portal, r.courseId, d)) },
        )
    }
    composable<AttendanceTakeRoute> {
        val r = it.toRoute<AttendanceTakeRoute>()
        AttendanceTakeScreen(Portal.of(r.portal), r.courseId, r.date, nav::popBackStack)
    }
    composable<AttendanceDayRoute> {
        val r = it.toRoute<AttendanceDayRoute>()
        AttendanceDayScreen(
            portal = Portal.of(r.portal), courseId = r.courseId, date = r.date, onBack = nav::popBackStack,
            onEdit = { nav.navigate(AttendanceTakeRoute(r.portal, r.courseId, r.date)) },
            onStudent = { e -> nav.navigate(StudentAttendanceRoute(r.portal, r.courseId, e)) },
        )
    }
    composable<StudentAttendanceRoute> {
        val r = it.toRoute<StudentAttendanceRoute>()
        StudentAttendanceScreen(Portal.of(r.portal), r.courseId, r.enrollmentId, nav::popBackStack)
    }

    composable<GradeListRoute> {
        val r = it.toRoute<GradeListRoute>()
        GradeListScreen(
            portal = Portal.of(r.portal), courseId = r.courseId, onBack = nav::popBackStack,
            onAdd = { nav.navigate(GradeFormRoute(r.portal, r.courseId)) },
            onEdit = { g -> nav.navigate(GradeFormRoute(r.portal, r.courseId, g)) },
        )
    }
    composable<GradeFormRoute> {
        val r = it.toRoute<GradeFormRoute>()
        GradeFormScreen(Portal.of(r.portal), r.courseId, r.gradeId, nav::popBackStack)
    }
    composable<StudentGradesRoute> {
        val r = it.toRoute<StudentGradesRoute>()
        StudentGradesScreen(Portal.of(r.portal), r.courseId, r.enrollmentId, nav::popBackStack)
    }
}
