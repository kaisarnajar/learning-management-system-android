package com.darsequran.academy.ui.manage.courses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.automirrored.rounded.Grading
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.darsequran.academy.data.staff.formOf
import com.darsequran.academy.ui.common.Load
import com.darsequran.academy.ui.common.Overline
import com.darsequran.academy.ui.common.ReloadOnReturn
import com.darsequran.academy.ui.common.StatusChip
import com.darsequran.academy.ui.common.Tone
import com.darsequran.academy.ui.common.rupees
import com.darsequran.academy.ui.manage.common.AddFab
import com.darsequran.academy.ui.manage.common.ConfirmDialog
import com.darsequran.academy.ui.manage.common.DetailCard
import com.darsequran.academy.ui.manage.common.FormCheckbox
import com.darsequran.academy.ui.manage.common.FormDateField
import com.darsequran.academy.ui.manage.common.FormDropdown
import com.darsequran.academy.ui.manage.common.FormNumberField
import com.darsequran.academy.ui.manage.common.FormScreen
import com.darsequran.academy.ui.manage.common.FormSection
import com.darsequran.academy.ui.manage.common.FormSwitch
import com.darsequran.academy.ui.manage.common.FormTextField
import com.darsequran.academy.ui.manage.common.LoadedColumn
import com.darsequran.academy.ui.manage.common.ManageListScreen
import com.darsequran.academy.ui.manage.common.ManageScaffold
import com.darsequran.academy.ui.manage.common.Notice
import com.darsequran.academy.ui.manage.common.RecordCard
import com.darsequran.academy.ui.manage.common.RowIcon
import com.darsequran.academy.ui.manage.common.rememberActionRunner
import com.darsequran.academy.ui.manage.common.rememberLoader
import com.darsequran.academy.ui.manage.common.rememberPaged

/** Course status → chip tone, as the website's CourseStatusBadge colours them. */
fun courseStatusTone(status: String): Tone = when (status) {
    "PUBLISHED" -> Tone.Info
    "ONGOING" -> Tone.Success
    "ON_HOLD", "COMING_SOON" -> Tone.Warning
    else -> Tone.Neutral
}

// ------------------------------------------------------------------ list (/admin/courses)

@Composable
fun CoursesAdminListScreen(onBack: () -> Unit, onOpen: (String) -> Unit, onAdd: () -> Unit) {
    val vm = rememberPaged("admin-courses") { page, q, _ -> service<AdminCoursesApi>().list(page, q.ifBlank { null }) }
    ReloadOnReturn(vm::refresh)
    ManageListScreen(
        title = "Courses",
        onBack = onBack,
        vm = vm,
        key = { it.id },
        emptyTitle = "No courses yet",
        searchHint = "Title, category or instructor",
        fab = { AddFab("Add course", onAdd) },
    ) { c ->
        RecordCard(
            title = c.title,
            subtitle = listOfNotNull(c.category, c.teacherName ?: "No instructor", c.duration?.takeIf { it.isNotBlank() }).joinToString(" · "),
            meta = "Registration ${rupees(c.registrationFeePaise)} · ${rupees(c.feePaise)} ${c.feeFrequencyLabel.lowercase()} · " +
                "${c.studentCount} student${if (c.studentCount == 1) "" else "s"}" + if (c.featured) " · Featured" else "",
            chip = c.statusLabel to courseStatusTone(c.status),
            leading = { RowIcon(Icons.AutoMirrored.Rounded.MenuBook) },
            onClick = { onOpen(c.id) },
        )
    }
}

// ------------------------------------------------------------------ course hub (course layout + its tabs)

/**
 * One course, as the website's course layout shows it (status, category, title, instructor), with its sections:
 * students, attendance, grade cards and announcements. Admins can also edit or delete the course here.
 */
@Composable
fun CourseHubScreen(
    portal: Portal,
    courseId: String,
    onBack: () -> Unit,
    onStudents: () -> Unit,
    onAttendance: () -> Unit,
    onGrades: () -> Unit,
    onAnnouncements: () -> Unit,
    onEdit: () -> Unit,
) {
    val loader = rememberLoader("course-hub-${portal.path}-$courseId") { service<CourseToolsApi>().course(portal.path, courseId).course }
    ReloadOnReturn { loader.reload() }
    val runner = rememberActionRunner()
    var confirmDelete by remember { mutableStateOf(false) }
    val admin = portal == Portal.Admin

    ManageScaffold("Course", onBack, runner.snackbar, actions = {
        if (admin) {
            IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, "Edit course") }
            IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.Delete, "Delete course") }
        }
    }) { padding ->
        LoadedColumn(loader, padding) { c ->
            item {
                DetailCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusChip(c.statusLabel, courseStatusTone(c.status))
                        Overline(c.category, modifier = Modifier.align(androidx.compose.ui.Alignment.CenterVertically))
                    }
                    Text(c.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    if (admin) Text(c.teacherName?.let { "Instructor: $it" } ?: "No instructor assigned", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else Text("${c.level} · Starts ${c.startDate ?: "—"}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!admin && !c.duration.isNullOrBlank()) Text("Duration: ${c.duration}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item { HubTile(Icons.Rounded.Groups, "Students", if (admin) "Roster, attendance and grade cards per student" else "Profiles, reports and private messages", onStudents) }
            item { HubTile(Icons.Rounded.EventAvailable, "Attendance", "Mark today's class and see the history", onAttendance) }
            item { HubTile(Icons.AutoMirrored.Rounded.Grading, "Grade cards", "Test reports and marks", onGrades) }
            item { HubTile(Icons.Rounded.Campaign, "Announcements", "Course-wide updates for enrolled students", onAnnouncements) }
        }
    }

    if (confirmDelete) ConfirmDialog(
        "Delete this course?",
        "This cannot be undone. It is refused while students are enrolled or the course has payment, attendance, grade or coupon records; set it to Completed or On hold instead.",
        "Delete", destructive = true,
        onConfirm = { runner.run({ service<AdminCoursesApi>().delete(courseId) }, success = "Course deleted.") { onBack() } },
        onDismiss = { confirmDelete = false },
    )
}

@Composable
private fun HubTile(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    RecordCard(title = title, subtitle = subtitle, leading = { RowIcon(icon) }, onClick = onClick)
}

// ------------------------------------------------------------------ add / edit (/admin/courses/new, /[id]/edit)

@Composable
fun CourseFormScreen(id: String?, onBack: () -> Unit) {
    val loader = rememberLoader("admin-course-form-${id ?: "new"}") {
        if (id == null) service<AdminCoursesApi>().newForm() else service<AdminCoursesApi>().get(id)
    }
    val runner = rememberActionRunner()
    val state by loader.state.collectAsStateWithLifecycle()
    val form = (state as? Load.Ready)?.value

    var loaded by rememberSaveable { mutableStateOf(false) }
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var startDate by rememberSaveable { mutableStateOf("") }
    var duration by rememberSaveable { mutableStateOf("") }
    var categorySelect by rememberSaveable { mutableStateOf("") }
    var customCategory by rememberSaveable { mutableStateOf("") }
    var teacherId by rememberSaveable { mutableStateOf("") }
    var level by rememberSaveable { mutableStateOf("Beginner") }
    var enrollmentFee by rememberSaveable { mutableStateOf("0") }
    var fee by rememberSaveable { mutableStateOf("") }
    var frequency by rememberSaveable { mutableStateOf("MONTHLY") }
    var status by rememberSaveable { mutableStateOf("PUBLISHED") }
    var featured by rememberSaveable { mutableStateOf(false) }
    var confirmReopen by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(form) {
        val v = form?.values ?: return@LaunchedEffect
        if (loaded) return@LaunchedEffect
        title = v.title; description = v.description; startDate = v.startDate; duration = v.duration
        val standard = v.category.isBlank() || v.category in form.options.categories
        categorySelect = if (standard) v.category else "Other"; customCategory = if (standard) "" else v.category
        teacherId = v.teacherId; level = v.level; enrollmentFee = v.enrollmentFeeInr.toString(); fee = v.monthlyFeeInr.toString()
        frequency = v.feeFrequency; status = v.status; featured = v.featuredOnHomepage
        loaded = true
    }

    val category = if (categorySelect == "Other") customCategory.trim() else categorySelect
    FormScreen(
        title = if (id == null) "Add course" else "Edit course",
        onBack = onBack,
        saving = runner.busy,
        snackbar = runner.snackbar,
        saveLabel = if (id == null) "Create course" else "Save changes",
        onSave = {
            val parts = formOf {
                text("title", title.trim()); text("description", description.trim()); text("startDate", startDate); text("duration", duration.trim())
                text("category", category); text("teacherId", teacherId); text("level", level)
                text("enrollmentFeeInr", enrollmentFee.ifBlank { "0" }); text("monthlyFeeInr", fee.ifBlank { "0" }); text("feeFrequency", frequency)
                text("status", status); check("featuredOnHomepage", featured && status != "DRAFT"); check("confirmReopen", confirmReopen)
            }
            runner.run(
                { if (id == null) service<AdminCoursesApi>().create(parts) else service<AdminCoursesApi>().update(id, parts) },
                success = if (id == null) "Course created." else "Course updated.",
            ) { onBack() }
        },
    ) {
        if (form == null) {
            when (val s = state) { is Load.Failed -> Notice(s.message); else -> Text("Loading…") }
            return@FormScreen
        }
        val o = form.options
        if (o.teachers.isEmpty()) Notice("Add at least one teacher before creating a course.", Tone.Warning)

        FormTextField("Title", title, { title = it }, isError = loaded && title.isNotEmpty() && title.trim().length < 3)
        FormTextField("Description", description, { description = it }, singleLine = false, minLines = 4)
        FormDateField("Start date", startDate, { startDate = it })
        FormTextField("Duration", duration, { duration = it }, supporting = "e.g. 6 months, 1 year. Shown on course listings and detail pages.", maxLength = 80)
        FormDropdown("Category", o.categories.map { it to it }, categorySelect, { categorySelect = it }, placeholder = "Select a category")
        if (categorySelect == "Other") FormTextField("Specify category", customCategory, { customCategory = it }, maxLength = 100)
        FormDropdown("Instructor", o.teachers.map { it.id to "${it.name} — ${it.specialization}" }, teacherId, { teacherId = it }, placeholder = "Select a teacher")
        FormDropdown("Level", o.levels.map { it.value to it.label }, level, { level = it }, supporting = "Used for display; fees are set below per course.")

        FormSection("Fees") {
            FormNumberField("Enrollment fee (₹)", enrollmentFee, { enrollmentFee = it }, supporting = "Use 0 when there is no one-time enrollment fee.")
            FormDropdown(
                "Fee frequency", o.feeFrequencies.map { it.value to it.label }, frequency, { frequency = it },
                supporting = if (frequency == "ONE_TIME") "Students pay once (no recurring billing)."
                else "Students pay ${(o.feeFrequencies.firstOrNull { it.value == frequency }?.label ?: "monthly").lowercase()} from their profile.",
            )
            FormNumberField("Fee amount (₹)", fee, { fee = it }, supporting = "Amount due per billing period (${(o.feeFrequencies.firstOrNull { it.value == frequency }?.label ?: "Monthly").lowercase()}).")
        }

        FormDropdown("Course status", o.statuses.map { it.value to "${it.label} — ${it.description}" }, status, { status = it })
        if (form.values.status == "COMPLETED" && status != "COMPLETED" && id != null) {
            FormCheckbox("Reopen this course anyway. Completed enrollments become active again; certificates already issued stay as they are.", confirmReopen) { confirmReopen = it }
        }

        val canFeature = form.values.featuredOnHomepage || o.featuredCount < o.featuredMax
        FormSwitch(
            "Show on homepage",
            featured && canFeature,
            { if (canFeature) featured = it },
            description = if (canFeature) "Featured in the Featured Courses section when not in draft (up to ${o.featuredMax}; ${o.featuredCount}/${o.featuredMax} slots used). All public courses still appear on the Courses page."
            else "There are already ${o.featuredMax} featured courses. Remove one course to add this course as a featured course.",
        )
        if (id != null) Text("Course ID: $id", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ------------------------------------------------------------------ roster (/admin/courses/[id]/students)

@Composable
fun CourseRosterScreen(courseId: String, onBack: () -> Unit, onAttendance: (String) -> Unit, onGrades: (String) -> Unit) {
    val vm = rememberPaged("admin-course-roster-$courseId") { page, q, _ -> service<AdminCoursesApi>().roster(courseId, page, q.ifBlank { null }) }
    ReloadOnReturn(vm::refresh)
    val header = rememberLoader("course-hub-admin-$courseId") { service<CourseToolsApi>().course(Portal.Admin.path, courseId).course }
    val headerState by header.state.collectAsStateWithLifecycle()
    val listState by vm.state.collectAsStateWithLifecycle()
    val runner = rememberActionRunner()
    var removing by remember { mutableStateOf<RosterRow?>(null) }
    val course = (headerState as? Load.Ready)?.value
    val completed = course?.status == "COMPLETED"

    ManageListScreen(
        title = "Students",
        onBack = onBack,
        vm = vm,
        key = { it.enrollmentId },
        emptyTitle = if (completed) "No completed students for this course yet" else "No active students enrolled yet",
        searchHint = "Search by name or email",
        snackbar = runner.snackbar,
        header = {
            if (course != null) item {
                Text(
                    "${course.title} · ${listState.total} ${if (completed) "completed" else "active"} student${if (listState.total == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    ) { s ->
        var menu by remember { mutableStateOf(false) }
        RecordCard(
            title = s.name ?: s.email,
            subtitle = "Roll no ${s.rollNumberLabel}" + if (s.name != null) " · ${s.email}" else "",
            leading = { RowIcon(Icons.Rounded.Groups) },
            onClick = { onAttendance(s.enrollmentId) },
            trailing = {
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "Actions") }
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem(text = { Text("Attendance") }, onClick = { menu = false; onAttendance(s.enrollmentId) })
                        DropdownMenuItem(text = { Text("Grade card") }, onClick = { menu = false; onGrades(s.enrollmentId) })
                        DropdownMenuItem(text = { Text("Remove", color = MaterialTheme.colorScheme.error) }, onClick = { menu = false; removing = s })
                    }
                }
            },
        )
    }

    removing?.let { s ->
        ConfirmDialog(
            "Remove enrollment",
            "Remove ${s.name ?: s.email} from this course? Their account will not be deleted; they can enroll again later.",
            "Remove", destructive = true,
            onConfirm = { runner.run({ service<AdminCoursesApi>().removeEnrollment(s.enrollmentId, courseId) }, success = "Enrollment removed.") { vm.refresh() } },
            onDismiss = { removing = null },
        )
    }
}

/** Small row of facts under a title (e.g. "Present 4 · Absent 1"). */
@Composable
internal fun FactRow(vararg facts: Pair<String, Tone>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        facts.forEach { (label, tone) -> StatusChip(label, tone) }
    }
}
