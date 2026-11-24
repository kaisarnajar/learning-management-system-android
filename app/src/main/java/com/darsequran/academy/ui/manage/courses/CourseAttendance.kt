package com.darsequran.academy.ui.manage.courses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.darsequran.academy.data.staff.courses.AdminCoursesApi
import com.darsequran.academy.data.staff.courses.AttendanceDay
import com.darsequran.academy.data.staff.courses.AttendanceMark
import com.darsequran.academy.data.staff.courses.AttendanceSave
import com.darsequran.academy.data.staff.courses.CourseToolsApi
import com.darsequran.academy.data.staff.courses.GradableStudent
import com.darsequran.academy.data.staff.courses.Portal
import com.darsequran.academy.data.staff.courses.ReportStudent
import com.darsequran.academy.ui.common.AppCard
import com.darsequran.academy.ui.common.EmptyState
import com.darsequran.academy.ui.common.ErrorState
import com.darsequran.academy.ui.common.Load
import com.darsequran.academy.ui.common.LoadingBox
import com.darsequran.academy.ui.common.PrimaryButton
import com.darsequran.academy.ui.common.ReloadOnReturn
import com.darsequran.academy.ui.common.StatusChip
import com.darsequran.academy.ui.common.Tone
import com.darsequran.academy.ui.common.academyToday
import com.darsequran.academy.ui.common.formatDate
import com.darsequran.academy.ui.manage.common.AddFab
import com.darsequran.academy.ui.manage.common.ConfirmDialog
import com.darsequran.academy.ui.manage.common.DetailCard
import com.darsequran.academy.ui.manage.common.FormDateField
import com.darsequran.academy.ui.manage.common.LoadedColumn
import com.darsequran.academy.ui.manage.common.ManageScaffold
import com.darsequran.academy.ui.manage.common.RecordCard
import com.darsequran.academy.ui.manage.common.RowIcon
import com.darsequran.academy.ui.manage.common.StatTile
import com.darsequran.academy.ui.manage.common.rememberActionRunner
import com.darsequran.academy.ui.manage.common.rememberLoader
import com.darsequran.academy.ui.theme.Brand
import retrofit2.HttpException

/*
 * Attendance (website: …/courses/[id]/attendance, /new, /[date], /[date]/edit, /student/[enrollmentId] and, for
 * admins, the attendance card PDF). Shared by the admin course pages and the teacher portal.
 */

// ------------------------------------------------------------------ history

@Composable
fun AttendanceListScreen(portal: Portal, courseId: String, onBack: () -> Unit, onTake: () -> Unit, onOpen: (String) -> Unit) {
    val loader = rememberLoader("attendance-${portal.path}-$courseId") { service<CourseToolsApi>().attendance(portal.path, courseId) }
    ReloadOnReturn { loader.reload() }
    val runner = rememberActionRunner()
    var deleting by remember { mutableStateOf<AttendanceDay?>(null) }

    ManageScaffold("Attendance", onBack, runner.snackbar, fab = { AddFab("Mark attendance", onTake, Icons.Rounded.EventAvailable) }) { padding ->
        LoadedColumn(loader, padding) { list ->
            list.course?.let { c -> item { Text(c.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) } }
            if (list.data.isEmpty()) item { EmptyState("No attendance yet", "No attendance records found for this course.", "Mark attendance", onTake) }
            items(list.data, key = { it.id }) { d ->
                RecordCard(
                    title = d.dateLabel,
                    subtitle = "${d.present} present · ${d.absent} absent",
                    meta = "${d.total} enrolled",
                    leading = { RowIcon(Icons.Rounded.EventAvailable) },
                    onClick = { onOpen(d.date) },
                    trailing = { IconButton(onClick = { deleting = d }) { Icon(Icons.Rounded.Delete, "Delete", tint = MaterialTheme.colorScheme.error) } },
                )
            }
        }
    }

    deleting?.let { d ->
        ConfirmDialog(
            "Delete attendance?", "Attendance for ${d.dateLabel} will be deleted for every student. This cannot be undone.", "Delete", destructive = true,
            onConfirm = { runner.run({ service<CourseToolsApi>().deleteAttendance(portal.path, courseId, d.date) }, success = "Attendance deleted.") { loader.reload() } },
            onDismiss = { deleting = null },
        )
    }
}

// ------------------------------------------------------------------ mark / edit (one tap per student)

/**
 * Marks a day's attendance: everyone starts present (or as saved, when editing [date]); tap a student to flip
 * them, "All present" / "All absent" for the whole class, then Save.
 */
@Composable
fun AttendanceTakeScreen(portal: Portal, courseId: String, date: String?, onBack: () -> Unit, onEditExisting: (String) -> Unit = {}) {
    val students = rememberLoader("attendance-students-${portal.path}-$courseId") { service<CourseToolsApi>().attendanceStudents(portal.path, courseId).data }
    val saved = if (date != null) rememberLoader("attendance-day-${portal.path}-$courseId-$date") { service<CourseToolsApi>().attendanceDay(portal.path, courseId, date) } else null
    val studentState by students.state.collectAsStateWithLifecycle()
    val savedState = saved?.state?.collectAsStateWithLifecycle()?.value
    val runner = rememberActionRunner()

    var day by rememberSaveable { mutableStateOf(date ?: academyToday().toString()) }
    // A new day that turns out to be marked already: saving would replace it for every student.
    var alreadyMarked by remember { mutableStateOf<String?>(null) }
    val entries = rememberRosterEntries<Boolean>("attendance-marks-${portal.path}-$courseId-${date ?: "new"}")
    val marks = entries.values
    val initialised = entries.initialised
    val roster = (studentState as? Load.Ready)?.value
    val savedDay = (savedState as? Load.Ready)?.value
    LaunchedEffect(roster, savedDay) {
        if (roster == null || (date != null && savedDay == null)) return@LaunchedEffect
        val previous = savedDay?.records?.associate { it.enrollmentId to (it.isPresent ?: true) }.orEmpty()
        entries.initialiseOnce { roster.associate { it.enrollmentId to (previous[it.enrollmentId] ?: true) } }
    }

    val present = roster?.count { marks[it.enrollmentId] ?: true } ?: 0
    val store = {
        val list = roster.orEmpty()
        runner.run(
            { service<CourseToolsApi>().saveAttendance(portal.path, courseId, day, AttendanceSave(list.map { AttendanceMark(it.enrollmentId, marks[it.enrollmentId] ?: true) })) },
            success = "Attendance saved.",
        ) { onBack() }
    }
    val save = {
        when {
            day.isBlank() -> runner.message("Please select a date.")
            date != null -> store() // editing that day on purpose
            else -> runner.run({ service<CourseToolsApi>().isMarked(portal.path, courseId, day) }) { marked ->
                if (marked) alreadyMarked = day else store()
            }
        }
    }

    ManageScaffold(
        if (date == null) "Mark attendance" else "Edit attendance",
        onBack, runner.snackbar,
        bottomBar = {
            if (!roster.isNullOrEmpty()) Surface(tonalElevation = 3.dp) {
                Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
                    PrimaryButton("Save attendance ($present of ${roster.size} present)", save, loading = runner.busy)
                }
            }
        },
    ) { padding ->
        val failed = (studentState as? Load.Failed)?.message ?: (savedState as? Load.Failed)?.message
        when {
            failed != null -> Box(Modifier.padding(padding)) { ErrorState(failed, { students.reload(true); saved?.reload(true) }) }
            roster == null || !initialised -> LoadingBox(Modifier.padding(padding).fillMaxSize())
            else -> LazyColumn(
                Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    if (date == null) FormDateField("Date", day, { day = it })
                    else Text("Date: ${savedDay?.dateLabel ?: date}", style = MaterialTheme.typography.titleMedium)
                }
                if (roster.isEmpty()) item { EmptyState("No active students", "No active students enrolled in this course yet.") }
                else {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(onClick = { roster.forEach { marks[it.enrollmentId] = true } }, modifier = Modifier.weight(1f)) { Text("All present") }
                            OutlinedButton(onClick = { roster.forEach { marks[it.enrollmentId] = false } }, modifier = Modifier.weight(1f)) { Text("All absent") }
                        }
                    }
                    item { Text("Tap a student to switch between present and absent.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    items(roster, key = { it.enrollmentId }) { s ->
                        val isPresent = marks[s.enrollmentId] ?: true
                        AttendanceToggleRow(s, isPresent) { marks[s.enrollmentId] = !isPresent }
                    }
                }
            }
        }
    }
    alreadyMarked?.let { d -> AlreadyMarkedDialog(d, onEdit = { alreadyMarked = null; onEditExisting(d) }, onDismiss = { alreadyMarked = null }) }
}

@Composable
private fun AlreadyMarkedDialog(day: String, onEdit: () -> Unit, onDismiss: () -> Unit) = ConfirmDialog(
    "Attendance already marked", "Attendance for ${formatDate(day)} is already saved. Saving here would replace it for every student. Open that day to change it instead.", "Open that day",
    onConfirm = onEdit,
    onDismiss = onDismiss,
)

/** Whether [date] already has attendance (the website answers 404 for a day with none). */
private suspend fun CourseToolsApi.isMarked(portal: String, courseId: String, date: String): Boolean = try {
    attendanceDay(portal, courseId, date).records.any { it.isPresent != null }
} catch (e: HttpException) {
    if (e.code() == 404) false else throw e
}

@Composable
private fun AttendanceToggleRow(s: GradableStudent, present: Boolean, onToggle: () -> Unit) {
    val bg = if (present) Brand.successContainer else Brand.dangerContainer
    val fg = if (present) Brand.success else Brand.danger
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(bg.copy(alpha = 0.45f)).clickable(onClick = onToggle).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(s.rollNumberLabel, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.widthIn(min = 32.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(s.name ?: s.email, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (s.name != null) Text(s.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(10.dp))
        Row(
            Modifier.clip(CircleShape).background(bg).padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(if (present) Icons.Rounded.Check else Icons.Rounded.Close, null, tint = fg, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(if (present) "Present" else "Absent", style = MaterialTheme.typography.labelLarge, color = fg)
        }
    }
}

// ------------------------------------------------------------------ one day

@Composable
fun AttendanceDayScreen(portal: Portal, courseId: String, date: String, onBack: () -> Unit, onEdit: () -> Unit, onStudent: (String) -> Unit) {
    val loader = rememberLoader("attendance-day-${portal.path}-$courseId-$date") { service<CourseToolsApi>().attendanceDay(portal.path, courseId, date) }
    ReloadOnReturn { loader.reload() }
    val runner = rememberActionRunner()
    var confirmDelete by remember { mutableStateOf(false) }

    ManageScaffold("Attendance record", onBack, runner.snackbar, actions = {
        IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, "Edit records") }
        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.Delete, "Delete") }
    }) { padding ->
        LoadedColumn(loader, padding) { d ->
            item {
                DetailCard(d.dateLabel) {
                    Text("Summary: ${d.present} Present, ${d.absent} Absent (${d.total} Total Enrolled)", style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (d.records.isEmpty()) item { EmptyState("No records", "No attendance records logged for this session.") }
            items(d.records, key = { it.enrollmentId }) { r ->
                RecordCard(
                    title = r.name ?: "Student",
                    subtitle = "Roll no ${r.rollNumberLabel} · ${r.email}",
                    trailing = { PresenceChip(r.isPresent == true) },
                    onClick = { onStudent(r.enrollmentId) },
                )
            }
        }
    }

    if (confirmDelete) ConfirmDialog(
        "Delete attendance?", "Attendance for this day will be deleted for every student. This cannot be undone.", "Delete", destructive = true,
        onConfirm = { runner.run({ service<CourseToolsApi>().deleteAttendance(portal.path, courseId, date) }, success = "Attendance deleted.") { onBack() } },
        onDismiss = { confirmDelete = false },
    )
}

@Composable
internal fun PresenceChip(present: Boolean) = StatusChip(if (present) "Present" else "Absent", if (present) Tone.Success else Tone.Danger)

// ------------------------------------------------------------------ one student

@Composable
fun StudentAttendanceScreen(portal: Portal, courseId: String, enrollmentId: String, onBack: () -> Unit) {
    val loader = rememberLoader("attendance-report-${portal.path}-$courseId-$enrollmentId") { service<CourseToolsApi>().attendanceReport(portal.path, courseId, enrollmentId) }
    ReloadOnReturn { loader.reload() }
    val runner = rememberActionRunner()

    ManageScaffold("Attendance report", onBack, runner.snackbar) { padding ->
        LoadedColumn(loader, padding) { r ->
            item { ReportHeader(r.student, r.courseTitle) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Sessions", r.summary.total.toString(), Modifier.weight(1f))
                    StatTile("Present", r.summary.present.toString(), Modifier.weight(1f))
                    StatTile("Absent", r.summary.absent.toString(), Modifier.weight(1f))
                    StatTile("Rate", "${r.summary.percentage}%", Modifier.weight(1f), highlight = r.summary.total > 0 && r.summary.percentage < 75)
                }
            }
            if (r.canDownloadCard) item { StudentCardActions(runner, courseId, enrollmentId, "attendance", "Attendance card", r.student) }
            if (r.records.isEmpty()) item { EmptyState("No attendance yet", "No attendance records found for this student yet.") }
            else item {
                AppCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 4.dp)) {
                    r.records.forEachIndexed { i, rec ->
                        if (i > 0) HorizontalDivider(color = Brand.outlineSoft)
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(rec.dateLabel, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            PresenceChip(rec.isPresent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ReportHeader(student: ReportStudent, courseTitle: String) {
    DetailCard {
        Text(student.name ?: "Student", style = MaterialTheme.typography.titleLarge)
        Text("${student.email} · Roll no ${student.rollNumberLabel}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(courseTitle, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    }
}

/** Admins: open the card PDF or e-mail it to the student (website: …/attendance-card, …/grade-card). */
@Composable
internal fun StudentCardActions(
    runner: com.darsequran.academy.ui.manage.common.ActionRunner,
    courseId: String,
    enrollmentId: String,
    kind: String,
    label: String,
    student: ReportStudent,
) {
    val context = LocalContext.current
    DetailCard(label) {
        Text("Official record issued by the academy, as a PDF.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    runner.openFile(
                        "${label.replace(' ', '_')}_${(student.name ?: "Student").replace(' ', '_')}.pdf",
                        fetch = { service<AdminCoursesApi>().card(courseId, kind, enrollmentId) }, context = context,
                    )
                },
                enabled = !runner.busy,
            ) { Icon(Icons.Rounded.Badge, null); Spacer(Modifier.width(6.dp)); Text("View") }
            OutlinedButton(
                onClick = { runner.run({ service<AdminCoursesApi>().emailCard(courseId, kind, enrollmentId) }, success = "$label sent. Ask the student to check their Inbox or Spam/Junk folder.") },
                enabled = !runner.busy,
            ) { Icon(Icons.Rounded.Email, null); Spacer(Modifier.width(6.dp)); Text("Send via email") }
        }
    }
}
