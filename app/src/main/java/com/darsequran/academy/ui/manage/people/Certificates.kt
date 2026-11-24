package com.darsequran.academy.ui.manage.people

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.darsequran.academy.data.staff.formOf
import com.darsequran.academy.ui.common.Load
import com.darsequran.academy.ui.common.ReloadOnReturn
import com.darsequran.academy.ui.common.Tone
import com.darsequran.academy.ui.common.formatDate
import com.darsequran.academy.ui.common.formatDateTime
import com.darsequran.academy.ui.manage.common.AddFab
import com.darsequran.academy.ui.manage.common.ConfirmDialog
import com.darsequran.academy.ui.manage.common.DetailCard
import com.darsequran.academy.ui.manage.common.Field
import com.darsequran.academy.ui.manage.common.FormDropdown
import com.darsequran.academy.ui.manage.common.FormNumberField
import com.darsequran.academy.ui.manage.common.FormScreen
import com.darsequran.academy.ui.manage.common.LoadedColumn
import com.darsequran.academy.ui.manage.common.ManageListScreen
import com.darsequran.academy.ui.manage.common.ManageScaffold
import com.darsequran.academy.ui.manage.common.Notice
import com.darsequran.academy.ui.manage.common.RecordCard
import com.darsequran.academy.ui.manage.common.RowIcon
import com.darsequran.academy.ui.manage.common.rememberActionRunner
import com.darsequran.academy.ui.manage.common.rememberLoader
import com.darsequran.academy.ui.manage.common.rememberPaged

private fun certificateFileName(c: CertificateRow) =
    "certificate-${(c.certificateNumber ?: c.enrollmentId).replace(Regex("[^A-Za-z0-9-]"), "-")}.pdf"

@Composable
fun CertificatesListScreen(onBack: () -> Unit, onOpen: (String) -> Unit, onGenerate: () -> Unit) {
    val vm = rememberPaged("admin-certificates") { page, q, _ -> service<CertificatesAdminApi>().list(page, q.ifBlank { null }) }
    ReloadOnReturn(vm::refresh)
    ManageListScreen(
        title = "Certificates",
        onBack = onBack,
        vm = vm,
        key = { it.enrollmentId },
        emptyTitle = "No certificates have been generated yet",
        searchHint = "Certificate number, name or course",
        fab = { AddFab("Generate", onGenerate) },
    ) { c ->
        RecordCard(
            title = c.user.name ?: c.user.email,
            subtitle = c.course.title,
            meta = listOfNotNull(c.certificateNumber, formatDate(c.certificateGeneratedAt).ifBlank { null }).joinToString(" · "),
            chip = (c.typeLabel + (c.certificateGrade?.let { " · grade $it" } ?: "")) to Tone.Info,
            leading = { RowIcon(Icons.Rounded.WorkspacePremium) },
            onClick = { onOpen(c.enrollmentId) },
        )
    }
}

/** /admin/certificates/[enrollmentId]: the certificate's facts, the PDF, "Send via Email" and delete. */
@Composable
fun CertificateDetailScreen(enrollmentId: String, onBack: () -> Unit) {
    val loader = rememberLoader("admin-certificate-$enrollmentId") { service<CertificatesAdminApi>().get(enrollmentId).certificate }
    ReloadOnReturn { loader.reload() }
    val runner = rememberActionRunner()
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }

    ManageScaffold("Student certificate", onBack, runner.snackbar, actions = {
        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.Delete, "Delete certificate") }
    }) { padding ->
        LoadedColumn(loader, padding) { c ->
            item {
                DetailCard {
                    Text(c.user.name ?: "N/A", style = MaterialTheme.typography.titleLarge)
                    Text(c.user.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { runner.openFile(certificateFileName(c), fetch = { service<CertificatesAdminApi>().pdf(enrollmentId) }, context = context) },
                            enabled = !runner.busy,
                        ) { Icon(Icons.Rounded.PictureAsPdf, null); Spacer(Modifier.width(6.dp)); Text("View PDF") }
                        OutlinedButton(
                            onClick = {
                                runner.run(
                                    { service<CertificatesAdminApi>().email(enrollmentId) },
                                    success = "Certificate sent successfully. Please check student's Inbox or Spam/Junk folder.",
                                ) { loader.reload() }
                            },
                            enabled = !runner.busy,
                        ) { Icon(Icons.Rounded.Email, null); Spacer(Modifier.width(6.dp)); Text("Send via Email") }
                    }
                    if (runner.busy) Text("Preparing the certificate PDF…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                DetailCard("Certificate") {
                    Field("Certificate number", c.certificateNumber)
                    Field("Course", c.course.title)
                    Field("Type", "Certificate of " + c.typeLabel)
                    Field("Grade", c.certificateGrade?.toString() ?: "N/A")
                    Field("Date generated", formatDate(c.certificateGeneratedAt))
                    Field("Last e-mailed", c.certificateEmailSentAt?.let(::formatDateTime), showEmpty = false)
                }
            }
        }
    }

    if (confirmDelete) ConfirmDialog(
        "Delete this certificate?",
        "The certificate and its PDF are removed. The enrollment stays and a new certificate can be generated.",
        "Delete", destructive = true,
        onConfirm = { runner.run({ service<CertificatesAdminApi>().delete(enrollmentId) }, success = "Certificate deleted successfully.") { onBack() } },
        onDismiss = { confirmDelete = false },
    )
}

/** /admin/certificates/new: course → student (enrollments without a certificate) → type → grade. */
@Composable
fun GenerateCertificateScreen(onBack: () -> Unit, onGenerated: (String) -> Unit) {
    val loader = rememberLoader("admin-certificate-candidates") { service<CertificatesAdminApi>().eligible().enrollments }
    val runner = rememberActionRunner()
    val state by loader.state.collectAsStateWithLifecycle()
    val candidates = (state as? Load.Ready)?.value.orEmpty()

    var courseId by rememberSaveable { mutableStateOf("") }
    var enrollmentId by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf("APPRECIATION") }
    var grade by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    val courses = candidates.map { it.course }.distinctBy { it.id }
    val students = candidates.filter { it.course.id == courseId }

    FormScreen(
        title = "Generate certificate",
        onBack = onBack,
        saving = runner.busy,
        snackbar = runner.snackbar,
        saveLabel = "Generate Certificate",
        error = error,
        onSave = {
            val g = grade.toIntOrNull()
            error = when {
                enrollmentId.isBlank() -> "Please select a student."
                type == "COMPLETION" && (g == null || g !in 0..10) -> "A whole-number grade from 0 to 10 is required for a Certificate of Completion."
                else -> null
            }
            if (error == null) runner.run({
                service<CertificatesAdminApi>().generate(formOf {
                    text("enrollmentId", enrollmentId); text("certificateType", type)
                    if (type == "COMPLETION") text("certificateGrade", grade)
                })
            }, success = "Certificate generated successfully.") { onGenerated(enrollmentId) }
        },
    ) {
        Text(
            "Issue a certificate of completion or appreciation for an enrolled student.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        when (val s = state) {
            is Load.Failed -> Notice(s.message)
            Load.Loading -> Text("Loading…")
            is Load.Ready -> if (s.value.isEmpty()) Notice("All active students currently have certificates generated. No pending enrollments.", Tone.Info)
        }
        FormDropdown("Select course", courses.map { it.id to it.title }, courseId, { courseId = it; enrollmentId = "" }, placeholder = "Choose course")
        FormDropdown(
            "Select student", students.map { it.id to (it.user.name ?: it.user.email) }, enrollmentId, { enrollmentId = it },
            placeholder = "Choose student", enabled = courseId.isNotBlank(),
            supporting = if (courseId.isNotBlank() && students.isEmpty()) "All students in this course already have certificates generated." else null,
        )
        FormDropdown(
            "Certificate type",
            listOf("APPRECIATION" to "Certificate of Appreciation", "COMPLETION" to "Certificate of Completion"),
            type, { type = it },
        )
        if (type == "COMPLETION") FormNumberField("Grade (0 to 10)", grade, { grade = it.take(2) }, supporting = "e.g. 9")
    }
}
