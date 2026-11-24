package com.darsequran.academy.ui.teach

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.automirrored.rounded.Grading
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.QuestionAnswer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.darsequran.academy.data.remote.Paged
import com.darsequran.academy.ui.common.AppCard
import com.darsequran.academy.ui.common.ReloadOnReturn
import com.darsequran.academy.ui.common.StatusChip
import com.darsequran.academy.ui.common.Tone
import com.darsequran.academy.ui.common.formatDate
import com.darsequran.academy.ui.common.statusLabel
import com.darsequran.academy.ui.manage.common.DetailCard
import com.darsequran.academy.ui.manage.common.Field
import com.darsequran.academy.ui.manage.common.LoadedColumn
import com.darsequran.academy.ui.manage.common.ManageListScreen
import com.darsequran.academy.ui.manage.common.ManageScaffold
import com.darsequran.academy.ui.manage.common.RecordCard
import com.darsequran.academy.ui.manage.common.RowIcon
import com.darsequran.academy.ui.manage.common.StatTile
import com.darsequran.academy.ui.manage.common.rememberLoader
import com.darsequran.academy.ui.manage.common.rememberPaged
import com.darsequran.academy.ui.manage.courses.CourseAnnouncementsRoute
import com.darsequran.academy.data.staff.courses.CourseHeader
import com.darsequran.academy.ui.manage.courses.CourseHubRoute
import com.darsequran.academy.data.staff.courses.Portal
import com.darsequran.academy.data.staff.courses.RosterRow
import com.darsequran.academy.ui.manage.courses.StudentAttendanceRoute
import com.darsequran.academy.ui.manage.courses.StudentGradesRoute
import com.darsequran.academy.ui.manage.courses.courseStatusTone
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/*
 * The teacher portal (website: /teacher, /teacher/courses/[id], …/students/[enrollmentId]). A course's attendance,
 * grade cards and announcements are the shared course tools in ui/manage/courses, opened with Portal.Teacher.
 */

// ------------------------------------------------------------------ API (/api/v1/teacher)

@Serializable
data class TeacherCourseRow(
    val id: String,
    val title: String = "",
    val status: String = "",
    val statusLabel: String = "",
    val level: String = "",
    val category: String = "",
    val startDate: String? = null,
    val duration: String? = null,
    val description: String = "",
    val studentCount: Int = 0,
)

@Serializable data class TeacherProfile(val name: String = "", val specialization: String = "", val email: String = "")
@Serializable data class TeacherStats(val totalCourses: Int = 0, val totalStudents: Int = 0)

@Serializable
data class TeacherHome(
    val data: List<TeacherCourseRow> = emptyList(),
    val totalCount: Int = 0,
    val teacher: TeacherProfile = TeacherProfile(),
    val stats: TeacherStats = TeacherStats(),
)

@Serializable
data class TeacherEnrollment(
    val id: String,
    val status: String = "",
    val statusLabel: String = "",
    val rollNumberLabel: String = "—",
    val createdAt: String? = null,
    val completedAt: String? = null,
)

@Serializable
data class TeacherStudentProfile(
    val id: String,
    val name: String = "",
    val email: String = "",
    val fatherName: String? = null,
    val dateOfBirthDisplay: String? = null,
    val occupationLabel: String? = null,
    val address: String? = null,
    val whatsapp: String? = null,
    val createdAt: String? = null,
)

@Serializable data class TeacherStudentResponse(val course: CourseHeader, val enrollment: TeacherEnrollment, val student: TeacherStudentProfile)

interface TeachApi {
    @GET("teacher/courses") suspend fun home(@Query("page") page: Int = 1): TeacherHome
    @GET("teacher/courses") suspend fun courses(@Query("page") page: Int): Paged<TeacherCourseRow>
    @GET("teacher/courses/{id}/students") suspend fun students(@Path("id") id: String, @Query("page") page: Int): Paged<RosterRow>
    @GET("teacher/courses/{id}/students/{enrollmentId}") suspend fun student(@Path("id") id: String, @Path("enrollmentId") enrollmentId: String): TeacherStudentResponse
}

// ------------------------------------------------------------------ routes

@Serializable data class TeacherStudentsRoute(val courseId: String)
@Serializable data class TeacherStudentRoute(val courseId: String, val enrollmentId: String)

/** The teacher portal's screens (the shared course tools are registered by coursesGraph). */
fun NavGraphBuilder.teachGraph(nav: NavHostController) {
    composable<TeacherStudentsRoute> {
        val r = it.toRoute<TeacherStudentsRoute>()
        TeacherStudentsScreen(r.courseId, nav::popBackStack) { e -> nav.navigate(TeacherStudentRoute(r.courseId, e)) }
    }
    composable<TeacherStudentRoute> {
        val r = it.toRoute<TeacherStudentRoute>()
        val teacher = Portal.Teacher.path
        TeacherStudentScreen(
            r.courseId, r.enrollmentId, nav::popBackStack,
            onAttendance = { nav.navigate(StudentAttendanceRoute(teacher, r.courseId, r.enrollmentId)) },
            onGrades = { nav.navigate(StudentGradesRoute(teacher, r.courseId, r.enrollmentId)) },
            onMessages = { name -> nav.navigate(CourseAnnouncementsRoute(teacher, r.courseId, r.enrollmentId, name)) },
        )
    }
}

// ------------------------------------------------------------------ home (/teacher)

/** The teacher's name and counts from the first page's response, kept across rotation. */
class TeachHeader : ViewModel() {
    var home by mutableStateOf<TeacherHome?>(null)
}

/** The teacher's tab: welcome and counts, shortcuts to their fatwa questions and blog posts, then their courses. */
@Composable
fun TeachHomeScreen(nav: NavHostController) {
    // The first page of courses and the welcome header come in one response: one request, not the same one twice.
    val header: TeachHeader = viewModel()
    val vm = rememberPaged("teacher-courses") { page, _, _ ->
        if (page == 1) service<TeachApi>().home().also { header.home = it }.let { Paged(it.data, it.totalCount, 1, it.data.size) }
        else service<TeachApi>().courses(page)
    }
    ReloadOnReturn { vm.refresh() }
    val home = header.home

    ManageListScreen(
        title = "Teach",
        onBack = null,
        vm = vm,
        key = { it.id },
        emptyTitle = "No courses are assigned to you yet. Contact the academy admin.",
        searchHint = null,
        header = {
            home?.let { h ->
                item {
                    DetailCard {
                        Text("Welcome back", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(h.teacher.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                        if (h.teacher.specialization.isNotBlank()) Text(h.teacher.specialization, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatTile("Courses", h.stats.totalCourses.toString(), Modifier.weight(1f))
                            StatTile("Students", h.stats.totalStudents.toString(), Modifier.weight(1f))
                        }
                        Field("Login email", h.teacher.email)
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ShortcutTile("Fatwa questions", Icons.Rounded.QuestionAnswer, Modifier.weight(1f)) { nav.navigate(TeacherFatwaRoute) }
                    ShortcutTile("Blog posts", Icons.Rounded.Edit, Modifier.weight(1f)) { nav.navigate(TeacherBlogsRoute) }
                }
            }
            item {
                Column {
                    Spacer(Modifier.height(8.dp))
                    Text("Your courses", style = MaterialTheme.typography.titleLarge)
                    Text("View students, mark attendance, add grade cards and post announcements.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
    ) { c ->
        AppCard(Modifier.fillMaxWidth(), onClick = { nav.navigate(CourseHubRoute(Portal.Teacher.path, c.id)) }) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip(c.statusLabel, courseStatusTone(c.status))
                StatusChip(c.level, Tone.Neutral)
            }
            Spacer(Modifier.height(8.dp))
            Text(c.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("${c.category} · Starts ${c.startDate ?: "—"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!c.duration.isNullOrBlank()) Text("Duration: ${c.duration}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (c.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(c.description, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(6.dp))
            Text("${c.studentCount} student${if (c.studentCount == 1) "" else "s"} enrolled", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun ShortcutTile(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    AppCard(modifier, onClick = onClick) {
        RowIcon(icon)
        Spacer(Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.titleSmall)
    }
}

// ------------------------------------------------------------------ students (/teacher/courses/[id])

@Composable
fun TeacherStudentsScreen(courseId: String, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val vm = rememberPaged("teacher-students-$courseId") { page, _, _ -> service<TeachApi>().students(courseId, page) }
    ReloadOnReturn(vm::refresh)
    val state by vm.state.collectAsStateWithLifecycle()
    ManageListScreen(
        title = "Students",
        onBack = onBack,
        vm = vm,
        key = { it.enrollmentId },
        emptyTitle = "No students enrolled in this course yet",
        searchHint = null,
        header = {
            item { Text("${state.total} student${if (state.total == 1) "" else "s"} enrolled", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        },
    ) { s ->
        RecordCard(
            title = s.name ?: s.email,
            subtitle = "Roll no ${s.rollNumberLabel}",
            chip = s.status?.let { (s.statusLabel ?: statusLabel(it).first) to statusLabel(it).second },
            leading = { RowIcon(Icons.Rounded.Groups) },
            onClick = { onOpen(s.enrollmentId) },
        )
    }
}

// ------------------------------------------------------------------ one student (/teacher/courses/[id]/students/[enrollmentId])

@Composable
fun TeacherStudentScreen(
    courseId: String,
    enrollmentId: String,
    onBack: () -> Unit,
    onAttendance: () -> Unit,
    onGrades: () -> Unit,
    onMessages: (String) -> Unit,
) {
    val loader = rememberLoader("teacher-student-$courseId-$enrollmentId") { service<TeachApi>().student(courseId, enrollmentId) }
    ReloadOnReturn { loader.reload() }
    ManageScaffold("Student profile", onBack) { padding ->
        LoadedColumn(loader, padding) { d ->
            val s = d.student
            item {
                DetailCard {
                    Text(s.name, style = MaterialTheme.typography.titleLarge)
                    Text(s.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onMessages(s.name) }) { Icon(Icons.Rounded.Mail, null); Spacer(Modifier.width(6.dp)); Text("Message") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onAttendance) { Icon(Icons.Rounded.EventAvailable, null); Spacer(Modifier.width(6.dp)); Text("Attendance") }
                        OutlinedButton(onClick = onGrades) { Icon(Icons.AutoMirrored.Rounded.Grading, null); Spacer(Modifier.width(6.dp)); Text("Grades") }
                    }
                }
            }
            item {
                DetailCard("Enrollment in ${d.course.title}") {
                    Column {
                        Text("Status", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        StatusChip(d.enrollment.statusLabel, statusLabel(d.enrollment.status).second)
                    }
                    Field("Roll no", d.enrollment.rollNumberLabel)
                    Field("Enrolled", formatDate(d.enrollment.createdAt))
                    Field("Completed", d.enrollment.completedAt?.let(::formatDate))
                }
            }
            item {
                DetailCard("Contact & profile") {
                    Field("Name", s.name)
                    Field("Father's name", s.fatherName)
                    Field("Date of birth", s.dateOfBirthDisplay)
                    Field("Occupation", s.occupationLabel)
                    Field("Address", s.address)
                    Field("WhatsApp", s.whatsapp)
                    Field("Email", s.email)
                    Field("Joined", formatDate(s.createdAt))
                }
            }
        }
    }
}
