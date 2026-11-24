package com.darsequran.academy.ui.manage.people

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.darsequran.academy.data.staff.formOf
import com.darsequran.academy.ui.common.Load
import com.darsequran.academy.ui.common.ReloadOnReturn
import com.darsequran.academy.ui.common.Tone
import com.darsequran.academy.ui.common.formatDate
import com.darsequran.academy.ui.manage.common.AddFab
import com.darsequran.academy.ui.manage.common.ConfirmDialog
import com.darsequran.academy.ui.manage.common.Field
import com.darsequran.academy.ui.manage.common.FilterOption
import com.darsequran.academy.ui.manage.common.FormDropdown
import com.darsequran.academy.ui.manage.common.FormScreen
import com.darsequran.academy.ui.manage.common.FormTextField
import com.darsequran.academy.ui.manage.common.ManageListScreen
import com.darsequran.academy.ui.manage.common.Notice
import com.darsequran.academy.ui.manage.common.rememberActionRunner
import com.darsequran.academy.ui.manage.common.rememberLoader
import com.darsequran.academy.ui.manage.common.rememberPaged
import com.darsequran.academy.ui.manage.common.AccountAnswer
import com.darsequran.academy.ui.manage.common.rememberAccountCheck

private const val PAID = "paid_awaiting"

private sealed interface EnrollmentAsk {
    val e: EnrollmentRequest
    data class Approve(override val e: EnrollmentRequest) : EnrollmentAsk
    data class Reject(override val e: EnrollmentRequest) : EnrollmentAsk
}

/**
 * /admin/enrollments: free-course requests to approve or reject, paid enrollments awaiting the fee (reject only;
 * the fee is verified under Payments), and "enrol a student" (the floating button).
 */
@Composable
fun EnrollmentsScreen(onBack: () -> Unit, onStudent: (String) -> Unit, onEnroll: () -> Unit, onPayments: (() -> Unit)?) {
    val vm = rememberPaged("admin-enrollments") { page, q, type -> service<EnrollmentsAdminApi>().list(page, q.ifBlank { null }, type) }
    val summary = rememberLoader("admin-enrollments-summary") { service<EnrollmentsAdminApi>().summary() }
    ReloadOnReturn { vm.refresh(); summary.reload() }
    val runner = rememberActionRunner()
    val listState by vm.state.collectAsStateWithLifecycle()
    val counts = (summary.state.collectAsStateWithLifecycle().value as? Load.Ready)?.value
    var asking by remember { mutableStateOf<EnrollmentAsk?>(null) }
    val paid = listState.filter == PAID

    ManageListScreen(
        title = "Course enrollments",
        onBack = onBack,
        vm = vm,
        key = { it.id },
        emptyTitle = if (paid) "No paid enrollments awaiting student payment" else "No free enrollment requests awaiting approval",
        searchHint = "Student name, e-mail or course",
        filters = listOf(
            FilterOption(null, "Free requests" + (counts?.freeCount?.takeIf { it > 0 }?.let { " ($it)" } ?: "")),
            FilterOption(PAID, "Paid, awaiting fee" + (counts?.paidCount?.takeIf { it > 0 }?.let { " ($it)" } ?: "")),
        ),
        snackbar = runner.snackbar,
        fab = { AddFab("Enrol a student", onEnroll) },
        header = {
            item {
                Text(
                    if (paid) "Students must pay the enrollment fee first. Once they submit payment, verify it under enrollment fee approvals — approving payment activates the enrollment automatically."
                    else "Courses with no enrollment fee. Approve to grant access immediately.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (paid && onPayments != null) item { OutlinedButton(onClick = onPayments) { Text("Go to enrollment fee approvals") } }
        },
    ) { e ->
        ActionCard(
            title = e.user.name ?: e.user.email,
            subtitle = listOfNotNull(e.user.email.takeIf { e.user.name != null }, e.courseTitle).joinToString(" · "),
            meta = "Requested " + formatDate(e.createdAt),
        ) {
            if (!paid) SmallPrimary("Approve", { asking = EnrollmentAsk.Approve(e) }, enabled = !runner.busy)
            SmallDanger("Reject", { asking = EnrollmentAsk.Reject(e) }, enabled = !runner.busy)
            e.user.id?.let { uid -> SmallOutlined("Student profile", { onStudent(uid) }) }
        }
    }

    asking?.let { ask ->
        val done = { vm.refresh(); summary.reload() }
        when (ask) {
            is EnrollmentAsk.Approve -> ConfirmDialog(
                "Approve enrollment", "Approve this student enrollment and notify them?", "Approve",
                onConfirm = {
                    runner.run(
                        { service<EnrollmentsAdminApi>().approve(ask.e.id, formOf { text("courseId", ask.e.courseId) }) },
                        success = "Enrollment approved. The student has been notified by email — if they don't see it, ask them to check their Spam/Junk folder.",
                    ) { done() }
                },
                onDismiss = { asking = null },
            )
            is EnrollmentAsk.Reject -> ConfirmDialog(
                "Reject enrollment", "Reject this enrollment request?", "Reject", destructive = true,
                onConfirm = {
                    runner.run(
                        { service<EnrollmentsAdminApi>().reject(ask.e.id, formOf { text("courseId", ask.e.courseId) }) },
                        success = "Enrollment rejected. The student has been notified by email — if they don't see it, ask them to check their Spam/Junk folder.",
                    ) { done() }
                },
                onDismiss = { asking = null },
            )
        }
    }
}

/** "Enroll a student manually": a registered account's e-mail and an open course; enrols with full access. */
@Composable
fun EnrollStudentScreen(onBack: () -> Unit) {
    val runner = rememberActionRunner()
    val summary = rememberLoader("admin-enrollments-summary") { service<EnrollmentsAdminApi>().summary() }
    val courses = (summary.state.collectAsStateWithLifecycle().value as? Load.Ready)?.value?.enrollableCourses.orEmpty()

    val account = rememberAccountCheck { e -> service<EnrollmentsAdminApi>().lookup(e).lookup.let { AccountAnswer(it.name?.takeIf { _ -> it.ok }, it.error.takeIf { _ -> !it.ok }) } }
    var courseId by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var done by rememberSaveable { mutableStateOf<String?>(null) }


    FormScreen(
        title = "Enrol a student",
        onBack = onBack,
        saving = runner.busy,
        snackbar = runner.snackbar,
        saveLabel = "Add enrollment",
        error = error,
        onSave = {
            error = when {
                account.email.isBlank() -> "Enter the student's e-mail."
                account.error != null -> account.error
                !account.verified -> "Check the account e-mail first."
                courseId.isBlank() -> "Select a course."
                else -> null
            }
            val email = account.email.trim()
            if (error == null) runner.run({
                service<EnrollmentsAdminApi>().enroll(formOf { text("email", email); text("courseId", courseId) })
            }) { r ->
                done = r.stringResult("success") ?: "Enrollment added."
                account.reset(); courseId = ""
                summary.reload()
            }
        },
    ) {
        Text(
            "The student must already have registered. Enter their e-mail and choose a course to enrol them directly with full access.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Notice(done, Tone.Success)
        FormTextField("Registered account email", account.email, { account.edit(it); done = null }, keyboardType = KeyboardType.Email)
        OutlinedButton(onClick = { account.verify() }, enabled = !account.checking && account.email.isNotBlank()) { Text(if (account.checking) "Checking…" else "Check account") }
        Notice(account.error, Tone.Warning)
        Field("Name (from account)", if (account.checking) "Checking account…" else account.name.ifBlank { "Check the e-mail above to load the account name." })
        FormDropdown("Course", courses.map { it.id to it.title }, courseId, { courseId = it }, placeholder = "Select a course")
    }
}
