package com.darsequran.academy.ui.manage.courses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.automirrored.rounded.Grading
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.darsequran.academy.ui.common.AppCard
import com.darsequran.academy.ui.common.EmptyState
import com.darsequran.academy.ui.common.Load
import com.darsequran.academy.ui.common.ReloadOnReturn
import com.darsequran.academy.ui.common.Tone
import com.darsequran.academy.ui.manage.common.AddFab
import com.darsequran.academy.ui.manage.common.ConfirmDialog
import com.darsequran.academy.ui.manage.common.FormDateField
import com.darsequran.academy.ui.manage.common.FormNumberField
import com.darsequran.academy.ui.manage.common.FormScreen
import com.darsequran.academy.ui.manage.common.FormTextField
import com.darsequran.academy.ui.manage.common.LoadedColumn
import com.darsequran.academy.ui.manage.common.ManageScaffold
import com.darsequran.academy.ui.manage.common.Notice
import com.darsequran.academy.ui.manage.common.RecordCard
import com.darsequran.academy.ui.manage.common.RowIcon
import com.darsequran.academy.ui.manage.common.StatTile
import com.darsequran.academy.ui.manage.common.rememberActionRunner
import com.darsequran.academy.ui.manage.common.rememberLoader
import com.darsequran.academy.ui.theme.Brand
import com.darsequran.academy.ui.common.academyToday
import java.math.BigDecimal
import java.math.RoundingMode

/*
 * Grade cards / test reports (website: …/courses/[id]/grades, /new, /[gradeId]/edit, /student/[enrollmentId] and,
 * for admins, the grade card PDF). Shared by the admin course pages and the teacher portal.
 */

/**
 * Marks with at most two decimals, without trailing zeros (as the website shows them). Always ASCII digits and a
 * "." whatever the phone's language: the text goes back into the marks field, which parses it with toDouble().
 */
fun formatMarks(marks: Double): String =
    BigDecimal.valueOf(marks).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

@Composable
fun GradeListScreen(portal: Portal, courseId: String, onBack: () -> Unit, onAdd: () -> Unit, onEdit: (String) -> Unit) {
    val loader = rememberLoader("grades-${portal.path}-$courseId") { service<CourseToolsApi>().grades(portal.path, courseId) }
    ReloadOnReturn { loader.reload() }
    val runner = rememberActionRunner()
    var deleting by remember { mutableStateOf<GradeCardRow?>(null) }

    ManageScaffold("Grade cards", onBack, runner.snackbar, fab = { AddFab("Add test report", onAdd) }) { padding ->
        LoadedColumn(loader, padding) { list ->
            list.course?.let { c -> item { Text(c.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) } }
            if (list.data.isEmpty()) item { EmptyState("No grade cards yet", "No grade cards found for this course.", "Add test report", onAdd) }
            items(list.data.size, key = { list.data[it].id }) { i ->
                val g = list.data[i]
                RecordCard(
                    title = g.title,
                    subtitle = "${g.dateLabel} · Max marks ${g.maxMarks}",
                    meta = "${g.graded} student${if (g.graded == 1) "" else "s"} graded",
                    leading = { RowIcon(Icons.AutoMirrored.Rounded.Grading) },
                    onClick = { onEdit(g.id) },
                    trailing = { IconButton(onClick = { deleting = g }) { Icon(Icons.Rounded.Delete, "Delete", tint = MaterialTheme.colorScheme.error) } },
                )
            }
        }
    }

    deleting?.let { g ->
        ConfirmDialog(
            "Delete grade card?", "Grade Card \"${g.title}\" and every student's marks in it will be deleted. This cannot be undone.", "Delete", destructive = true,
            onConfirm = { runner.run({ service<CourseToolsApi>().deleteGrade(portal.path, courseId, g.id) }, success = "Grade card deleted.") { loader.reload() } },
            onDismiss = { deleting = null },
        )
    }
}

/** New test report or edit of one: title, date, maximum marks and each active student's marks (blank = not graded). */
@Composable
fun GradeFormScreen(portal: Portal, courseId: String, gradeId: String?, onBack: () -> Unit) {
    val students = rememberLoader("grade-students-${portal.path}-$courseId") { service<CourseToolsApi>().gradeStudents(portal.path, courseId).data }
    val existing = if (gradeId != null) rememberLoader("grade-${portal.path}-$courseId-$gradeId") { service<CourseToolsApi>().grade(portal.path, courseId, gradeId).grade } else null
    val studentState by students.state.collectAsStateWithLifecycle()
    val gradeState = existing?.state?.collectAsStateWithLifecycle()?.value
    val runner = rememberActionRunner()

    val roster = (studentState as? Load.Ready)?.value
    val grade = (gradeState as? Load.Ready)?.value
    var title by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(academyToday().toString()) }
    var maxMarks by rememberSaveable { mutableStateOf("") }
    val entries = rememberRosterEntries<String>("grade-marks-${portal.path}-$courseId-${gradeId ?: "new"}")
    val marks = entries.values
    val initialised = entries.initialised
    LaunchedEffect(roster, grade) {
        if (entries.initialised || roster == null || (gradeId != null && grade == null)) return@LaunchedEffect
        if (grade != null) { title = grade.title; date = grade.date; maxMarks = grade.maxMarks.toString() }
        val previous = grade?.records?.associate { it.enrollmentId to it.marksObtained }.orEmpty()
        entries.initialiseOnce { roster.associate { s -> s.enrollmentId to (previous[s.enrollmentId]?.let(::formatMarks) ?: "") } }
    }

    val max = maxMarks.toIntOrNull()
    fun validate(): String? {
        if (date.isBlank()) return "Please select a date."
        if (title.isBlank()) return "Please provide an exam title."
        if (max == null || max <= 0) return "Maximum marks must be a whole number greater than 0."
        val values = roster.orEmpty().mapNotNull { marks[it.enrollmentId]?.takeIf(String::isNotBlank) }
        if (values.any { it.toDoubleOrNull() == null }) return "Marks must be numbers."
        if (values.any { (it.toDouble()) < 0 || it.toDouble() > max }) return "Marks must be between 0 and $max."
        return null
    }

    val failed = (studentState as? Load.Failed)?.message ?: (gradeState as? Load.Failed)?.message
    FormScreen(
        title = if (gradeId == null) "Add test report" else "Edit grade card",
        onBack = onBack,
        saving = runner.busy,
        snackbar = runner.snackbar,
        saveLabel = "Save grades",
        error = failed,
        onSave = {
            val problem = validate()
            if (problem != null) runner.message(problem)
            else if (roster.isNullOrEmpty()) runner.message("No active students enrolled in this course yet.")
            else {
                val body = GradeSave(
                    title = title.trim(), date = date, maxMarks = max!!,
                    records = roster.map { s -> GradeMark(s.enrollmentId, marks[s.enrollmentId]?.takeIf(String::isNotBlank)?.toDouble()) },
                )
                runner.run(
                    { if (gradeId == null) service<CourseToolsApi>().createGrade(portal.path, courseId, body) else service<CourseToolsApi>().updateGrade(portal.path, courseId, gradeId, body) },
                    success = "Grades saved.",
                ) { onBack() }
            }
        },
    ) {
        if (roster == null || !initialised) { if (failed == null) Text("Loading…"); return@FormScreen }
        Text(
            if (gradeId == null) "Record exam performance for enrolled students." else "Update exam performance for enrolled students.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FormTextField("Exam title", title, { title = it }, supporting = "e.g. Mid-Term", maxLength = 200)
        FormDateField("Date", date, { date = it })
        FormNumberField("Max marks", maxMarks, { maxMarks = it })
        if (roster.isEmpty()) Notice("No active students enrolled in this course yet.", Tone.Info)
        else {
            Text("Marks obtained (leave blank when not graded)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            AppCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 4.dp)) {
                roster.forEachIndexed { i, s ->
                    if (i > 0) HorizontalDivider(color = Brand.outlineSoft)
                    val value = marks[s.enrollmentId] ?: ""
                    val bad = value.isNotBlank() && (value.toDoubleOrNull()?.let { it < 0 || (max != null && it > max) } ?: true)
                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(s.rollNumberLabel, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.widthIn(min = 30.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(s.name ?: s.email, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        OutlinedTextField(
                            value = value,
                            onValueChange = { v ->
                                val clean = v.filter { it.isDigit() || it == '.' }
                                if (clean.count { it == '.' } <= 1 && clean.substringAfter('.', "").length <= 2) marks[s.enrollmentId] = clean
                            },
                            placeholder = { Text("—") },
                            suffix = { if (max != null) Text("/$max") },
                            singleLine = true,
                            isError = bad,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.width(120.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StudentGradesScreen(portal: Portal, courseId: String, enrollmentId: String, onBack: () -> Unit) {
    val loader = rememberLoader("grade-report-${portal.path}-$courseId-$enrollmentId") { service<CourseToolsApi>().gradeReport(portal.path, courseId, enrollmentId) }
    ReloadOnReturn { loader.reload() }
    val runner = rememberActionRunner()

    ManageScaffold("Grade report", onBack, runner.snackbar) { padding ->
        LoadedColumn(loader, padding) { r ->
            item { ReportHeader(r.student, r.courseTitle) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Exams", r.summary.exams.toString(), Modifier.weight(1f))
                    StatTile("Obtained", formatMarks(r.summary.obtained), Modifier.weight(1f))
                    StatTile("Max", r.summary.max.toString(), Modifier.weight(1f))
                    StatTile("Percentage", "${r.summary.percentage}%", Modifier.weight(1f), highlight = r.summary.max > 0 && r.summary.percentage < 40)
                }
            }
            if (r.canDownloadCard) item { StudentCardActions(runner, courseId, enrollmentId, "grades", "Grade card", r.student) }
            if (r.records.isEmpty()) item { EmptyState("No grades yet", "No grade records found for this student yet.") }
            else item {
                AppCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 4.dp)) {
                    r.records.forEachIndexed { i, rec ->
                        if (i > 0) HorizontalDivider(color = Brand.outlineSoft)
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(rec.title, style = MaterialTheme.typography.bodyLarge)
                                Text(rec.dateLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("${formatMarks(rec.marksObtained)} / ${rec.maxMarks}", style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            }
        }
    }
}
