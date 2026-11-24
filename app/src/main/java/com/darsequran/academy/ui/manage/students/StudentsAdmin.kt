package com.darsequran.academy.ui.manage.students

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.darsequran.academy.data.repo.orNull
import com.darsequran.academy.data.staff.formOf
import com.darsequran.academy.data.staff.people.EnrollmentsAdminApi
import com.darsequran.academy.data.staff.students.StudentEnrollment
import com.darsequran.academy.data.staff.students.StudentsAdminApi
import com.darsequran.academy.ui.common.Avatar
import com.darsequran.academy.ui.common.Load
import com.darsequran.academy.ui.common.ReloadOnReturn
import com.darsequran.academy.ui.common.Tone
import com.darsequran.academy.ui.common.formatDate
import com.darsequran.academy.ui.common.rupees
import com.darsequran.academy.ui.common.statusLabel
import com.darsequran.academy.ui.manage.common.ConfirmDialog
import com.darsequran.academy.ui.manage.common.DetailCard
import com.darsequran.academy.ui.manage.common.Field
import com.darsequran.academy.ui.manage.common.FieldRow
import com.darsequran.academy.ui.manage.common.FilterOption
import com.darsequran.academy.ui.manage.common.FormCheckbox
import com.darsequran.academy.ui.manage.common.FormDateField
import com.darsequran.academy.ui.manage.common.FormDropdown
import com.darsequran.academy.ui.manage.common.FormScreen
import com.darsequran.academy.ui.manage.common.FormTextField
import com.darsequran.academy.ui.manage.common.LoadedColumn
import com.darsequran.academy.ui.manage.common.ManageListScreen
import com.darsequran.academy.ui.manage.common.ManageScaffold
import com.darsequran.academy.ui.manage.common.RecordCard
import com.darsequran.academy.ui.manage.common.dateOnly
import com.darsequran.academy.ui.manage.common.rememberActionRunner
import com.darsequran.academy.ui.manage.common.rememberContainer
import com.darsequran.academy.ui.manage.common.rememberLoader
import com.darsequran.academy.ui.manage.common.rememberPaged
import kotlinx.serialization.Serializable

// ------------------------------------------------------------------ API (/api/v1/admin/students)

@Serializable data object StudentsAdminRoute
@Serializable data class StudentAdminRoute(val id: String)
@Serializable data class StudentEditRoute(val id: String)

fun NavGraphBuilder.studentsAdminGraph(nav: NavHostController) {
    composable<StudentsAdminRoute> { StudentsListScreen(nav::popBackStack) { nav.navigate(StudentAdminRoute(it)) } }
    composable<StudentAdminRoute> {
        val id = it.toRoute<StudentAdminRoute>().id
        StudentDetailScreen(id, nav::popBackStack, onEdit = { nav.navigate(StudentEditRoute(id)) })
    }
    composable<StudentEditRoute> { StudentEditScreen(it.toRoute<StudentEditRoute>().id, nav::popBackStack) }
}

// -------------------------------------------------------------- screens

private val sorts = listOf(
    FilterOption(null, "Reg. no"),
    FilterOption("name_asc", "Name A–Z"),
    FilterOption("date_desc", "Newest"),
    FilterOption("date_asc", "Oldest"),
    FilterOption("email_asc", "E-mail"),
)

@Composable
fun StudentsListScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val vm = rememberPaged("admin-students") { page, q, sort -> service<StudentsAdminApi>().list(page, q.ifBlank { null }, sort) }
    ReloadOnReturn(vm::refresh)
    ManageListScreen(
        title = "Students",
        onBack = onBack,
        vm = vm,
        key = { it.id },
        emptyTitle = "No students found",
        searchHint = "Registration no, name or e-mail",
        filters = sorts,
    ) { s ->
        RecordCard(
            title = s.name ?: s.email,
            subtitle = listOfNotNull(s.registrationNumber, s.email.takeIf { s.name != null }).joinToString(" · "),
            meta = s.createdAt?.let { "Joined " + formatDate(it) },
            leading = { Avatar(s.name ?: s.email, s.image) },
            onClick = { onOpen(s.id) },
        )
    }
}

@Composable
fun StudentDetailScreen(id: String, onBack: () -> Unit, onEdit: () -> Unit) {
    val loader = rememberLoader("admin-student-$id") { service<StudentsAdminApi>().get(id).student }
    val payments = rememberLoader("admin-student-payments-$id") { service<StudentsAdminApi>().payments(id) }
    ReloadOnReturn { loader.reload(); payments.reload() }
    val runner = rememberActionRunner()
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf<StudentEnrollment?>(null) }
    val state by loader.state.collectAsStateWithLifecycle()
    val paymentState by payments.state.collectAsStateWithLifecycle()

    ManageScaffold("Student", onBack, runner.snackbar, actions = {
        IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, "Edit student") }
        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.Delete, "Delete account") }
    }) { padding ->
        LoadedColumn(loader, padding) { s ->
            item {
                DetailCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(s.name ?: s.email, s.image, 64.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(s.name ?: "No name", style = MaterialTheme.typography.titleLarge)
                            Text(s.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            s.registrationNumber?.let { Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
                        }
                    }
                    if (!s.profileComplete) com.darsequran.academy.ui.common.Banner("Profile incomplete: the ID card is available once the student completes it.", Tone.Warning)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { runner.openFile("ID_Card_${(s.name ?: "Student").replace(' ', '_')}.pdf", fetch = { service<StudentsAdminApi>().idCard(id) }, context = context) },
                            enabled = s.profileComplete && !runner.busy,
                        ) { Icon(Icons.Rounded.Badge, null); Spacer(Modifier.width(6.dp)); Text("ID card") }
                        OutlinedButton(
                            onClick = { runner.run({ service<StudentsAdminApi>().emailIdCard(id) }, success = "ID card e-mailed to ${s.email}.") },
                            enabled = s.profileComplete && !runner.busy,
                        ) { Icon(Icons.Rounded.Email, null); Spacer(Modifier.width(6.dp)); Text("E-mail it") }
                    }
                }
            }
            item {
                DetailCard("Profile") {
                    Field("Gender", when (s.gender) { "MALE" -> "Male"; "FEMALE" -> "Female"; else -> null })
                    Field("Father's name", s.fatherName)
                    Field("Date of birth", s.dateOfBirthDisplay)
                    Field("Occupation", s.occupationLabel)
                    Field("Address", s.address)
                    Field("WhatsApp", s.whatsapp)
                    Field("Joined", s.createdAt?.let(::formatDate))
                }
            }
            item {
                DetailCard("Course enrollments") {
                    if (s.enrollments.isEmpty()) Text("Not enrolled in any course.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    s.enrollments.forEach { e ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(e.courseTitle, style = MaterialTheme.typography.titleSmall)
                                Text("Roll no ${e.rollNumber ?: "—"} · since ${formatDate(e.createdAt)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(4.dp))
                                com.darsequran.academy.ui.common.StatusChip(e.statusLabel.ifBlank { statusLabel(e.status).first }, statusLabel(e.status).second)
                            }
                            TextButton(onClick = { removing = e }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
            item {
                DetailCard("Payment history") {
                    when (val p = paymentState) {
                        is Load.Ready -> {
                            if (p.value.data.isEmpty()) Text("No payments recorded yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            p.value.data.forEach { pay ->
                                Column {
                                    FieldRow(formatDate(pay.paidAt), rupees(pay.amountInrPaise), strong = true)
                                    Text(listOfNotNull(pay.courseTitle, pay.description).joinToString(" · ").ifBlank { "—" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            if (p.value.totalCount > p.value.data.size) Text("Showing the latest ${p.value.data.size} of ${p.value.totalCount}.", style = MaterialTheme.typography.labelSmall)
                        }
                        is Load.Failed -> Text(p.message, color = MaterialTheme.colorScheme.error)
                        Load.Loading -> Text("Loading…", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    if (confirmDelete) ConfirmDialog(
        "Delete this account?",
        "The account is removed and its e-mail freed; payment and course records are kept for auditing. Refused while the student has active enrollments, pending fees or payment records.",
        "Delete", destructive = true,
        onConfirm = { runner.run({ service<StudentsAdminApi>().delete(id) }, success = "Student account removed.") { onBack() } },
        onDismiss = { confirmDelete = false },
    )
    removing?.let { e ->
        ConfirmDialog(
            "Remove from ${e.courseTitle}?",
            "The student's account is not deleted; they can enrol again later.",
            "Remove", destructive = true,
            onConfirm = { runner.run({ service<EnrollmentsAdminApi>().removeEnrollment(e.id, e.courseId) }, success = "Enrollment removed.") { loader.reload() } },
            onDismiss = { removing = null },
        )
    }
}

@Composable
fun StudentEditScreen(id: String, onBack: () -> Unit) {
    val loader = rememberLoader("admin-student-$id") { service<StudentsAdminApi>().get(id).student }
    val container = rememberContainer()
    val runner = rememberActionRunner()
    val state by loader.state.collectAsStateWithLifecycle()
    var occupations by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    LaunchedEffect(Unit) { occupations = container.repo.appConfig().orNull()?.lookups?.occupations.orEmpty().map { it.value to it.label } }

    val student = (state as? Load.Ready)?.value
    var loaded by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var father by rememberSaveable { mutableStateOf("") }
    var dob by rememberSaveable { mutableStateOf("") }
    var occupation by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var whatsapp by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableStateOf("") }
    var replacePhoto by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(student) {
        if (student != null && !loaded) {
            name = student.name.orEmpty(); father = student.fatherName.orEmpty(); dob = dateOnly(student.dateOfBirth)
            occupation = student.occupation.orEmpty(); address = student.address.orEmpty(); whatsapp = student.whatsapp.orEmpty()
            gender = student.gender.orEmpty(); loaded = true
        }
    }
    val hasUploadedPhoto = student?.image?.let { it.isNotBlank() && !it.startsWith("/assets/") } == true

    FormScreen(
        "Edit student", onBack, runner.busy, snackbar = runner.snackbar,
        canSave = loaded, loadError = (state as? Load.Failed)?.message, onRetry = { loader.reload(true) },
        onSave = {
        runner.run({
            service<StudentsAdminApi>().update(id, formOf {
                text("name", name); text("fatherName", father); text("dateOfBirth", dob); text("occupation", occupation)
                text("address", address); text("whatsapp", whatsapp); text("gender", gender); check("confirmReplacePhoto", replacePhoto)
            })
        }, success = "Student details updated.") { onBack() }
    }) {
        if (student == null) { Text("Loading…"); return@FormScreen }
        FormTextField("Full name", name, { name = it })
        FormTextField("Father's name", father, { father = it })
        FormDateField("Date of birth", dob, { dob = it })
        FormDropdown("Gender", listOf("MALE" to "Male", "FEMALE" to "Female"), gender, { gender = it })
        if (gender == "FEMALE" && hasUploadedPhoto) FormCheckbox("Replace the uploaded photo with the female avatar", replacePhoto) { replacePhoto = it }
        FormDropdown("Occupation", occupations, occupation, { occupation = it })
        FormTextField("WhatsApp number", whatsapp, { whatsapp = it }, keyboardType = KeyboardType.Phone, supporting = "With country code, e.g. +919876543210")
        FormTextField("Address", address, { address = it }, singleLine = false, minLines = 3)
    }
}
