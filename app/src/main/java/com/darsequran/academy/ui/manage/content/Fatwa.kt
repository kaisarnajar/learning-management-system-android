package com.darsequran.academy.ui.manage.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.QuestionAnswer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.darsequran.academy.data.staff.ActionResponse
import com.darsequran.academy.data.staff.formOf
import com.darsequran.academy.ui.common.Banner
import com.darsequran.academy.ui.common.HtmlText
import com.darsequran.academy.ui.common.Load
import com.darsequran.academy.ui.common.PrimaryButton
import com.darsequran.academy.ui.common.ReloadOnReturn
import com.darsequran.academy.ui.common.StatusChip
import com.darsequran.academy.ui.common.Tone
import com.darsequran.academy.ui.common.formatDate
import com.darsequran.academy.ui.common.openWeb
import com.darsequran.academy.ui.manage.common.ConfirmDialog
import com.darsequran.academy.ui.manage.common.DetailCard
import com.darsequran.academy.ui.manage.common.FilterOption
import com.darsequran.academy.ui.manage.common.FormSwitch
import com.darsequran.academy.ui.manage.common.LoadedColumn
import com.darsequran.academy.ui.manage.common.ManageListScreen
import com.darsequran.academy.ui.manage.common.ManageScaffold
import com.darsequran.academy.ui.manage.common.RecordCard
import com.darsequran.academy.ui.manage.common.RichTextField
import com.darsequran.academy.ui.manage.common.RowIcon
import com.darsequran.academy.ui.manage.common.rememberActionRunner
import com.darsequran.academy.ui.manage.common.rememberLoader
import com.darsequran.academy.ui.manage.common.rememberPaged

// Website: /admin/fatwa (+ [id]) for admins, /teacher/fatwa (+ [id]) for teachers. Both answer with the same editor.

enum class FatwaAudience { Admin, Teacher }

private val fatwaFilters = listOf(
    FilterOption(null, "All"),
    FilterOption("unanswered", "Unanswered"),
    FilterOption("pending", "Pending Review"),
    FilterOption("answered", "Answered"),
    FilterOption("rejected", "Rejected"),
)

@Composable
fun FatwaListScreen(audience: FatwaAudience, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val admin = audience == FatwaAudience.Admin
    val vm = rememberPaged(if (admin) "admin-fatwa" else "teacher-fatwa") { page, q, filter ->
        if (admin) service<ContentApi>().fatwas(page, q.ifBlank { null }, filter) else service<ContentApi>().teacherFatwas(page, q.ifBlank { null }, filter)
    }
    ReloadOnReturn(vm::refresh)
    val runner = rememberActionRunner()
    val context = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()
    var deleting by remember { mutableStateOf<FatwaRow?>(null) }

    ManageListScreen(
        title = "Fatwa Q&A",
        onBack = onBack,
        vm = vm,
        key = { it.id },
        emptyTitle = if (admin) "No questions yet" else "No questions available",
        searchHint = "Search by subject, asker, e-mail or category".takeIf { admin } ?: "Search by subject, question or category",
        filters = fatwaFilters,
        snackbar = runner.snackbar,
        header = {
            item { Text("${state.total} questions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        },
    ) { q ->
        val action = when {
            q.approvalStatus == "PENDING" -> "Review"
            !q.answered -> "Answer"
            admin -> "Edit"
            else -> "View/Edit"
        }
        RecordCard(
            title = q.title,
            subtitle = if (admin) listOfNotNull(q.category, q.askerName, q.askerEmail ?: "N/A").joinToString(" · ") else q.category,
            meta = "Submitted " + formatDate(q.createdAt) + if (admin) " · " + (if (q.featuredOnHomepage) "Featured" else "Not featured") else "",
            chip = q.statusLabel to contentTone(q.statusLabel),
            leading = { RowIcon(Icons.Rounded.QuestionAnswer) },
            trailing = {
                RowMenu(
                    listOfNotNull(
                        RowAction(action) { onOpen(q.id) },
                        q.publicPath?.let { path -> RowAction("View on website") { openWeb(context, path) } },
                        if (admin) RowAction("Delete", destructive = true) { deleting = q } else null,
                    ),
                )
            },
            onClick = { onOpen(q.id) },
        )
    }
    deleting?.let { q ->
        DeleteFatwaDialog(q.title, { deleting = null }) { runner.run({ service<ContentApi>().deleteFatwa(q.id) }, success = "Question deleted.") { vm.refresh() } }
    }
}

@Composable
private fun DeleteFatwaDialog(title: String, onDismiss: () -> Unit, onConfirm: () -> Unit) =
    ConfirmDialog("Delete this question?", "\"$title\" and its answer are removed for good.", "Delete", onConfirm, onDismiss, destructive = true)

/** What the website's toast says after an admin publishes an answer (`email=` on its redirect). */
private fun answerPublishedMessage(response: ActionResponse): String {
    val email = response.redirect?.substringAfter("email=", "")?.substringBefore('&').orEmpty()
    return "Answer published. " + when (email) {
        "anonymous" -> "No notification e-mail is sent for an anonymous question."
        "failed" -> "The notification e-mail could not be sent. Check the SMTP settings."
        "skipped" -> "SMTP is not configured, so no e-mail was sent."
        else -> "The asker has been notified by e-mail (ask them to check Spam if it does not arrive)."
    }
}

/**
 * Answer a question. Admins publish (or update) the answer, approve or reject a teacher's pending one, feature it
 * on the homepage and delete the question; teachers draft an answer for review (locked while pending and once
 * approved).
 */
@Composable
fun FatwaAnswerScreen(audience: FatwaAudience, id: String, onBack: () -> Unit) {
    val admin = audience == FatwaAudience.Admin
    val loader = rememberLoader("fatwa-answer-$audience-$id") { if (admin) service<ContentApi>().fatwa(id) else service<ContentApi>().teacherFatwa(id) }
    val runner = rememberActionRunner()
    val context = LocalContext.current
    val state by loader.state.collectAsStateWithLifecycle()
    var answer by rememberSaveable { mutableStateOf<String?>(null) }
    var featured by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var confirmReject by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    LaunchedEffect(state) {
        val q = (state as? Load.Ready)?.value?.question ?: return@LaunchedEffect
        if (answer == null) answer = q.answer.orEmpty()
        if (featured == null) featured = q.featuredOnHomepage
    }

    val q = (state as? Load.Ready)?.value?.question
    val title = when {
        q == null -> "Fatwa question"
        !admin -> if (q.answered) "Edit draft answer" else "Draft answer"
        q.approvalStatus == "PENDING" -> "Review pending answer"
        q.answered -> "Edit answer"
        else -> "Answer question"
    }

    ManageScaffold(title, onBack, runner.snackbar, actions = {
        if (admin) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.Delete, "Delete question") }
    }) { padding ->
        LoadedColumn(loader, padding) { data ->
            val question = data.question
            val pending = question.approvalStatus == "PENDING"
            val editable = if (admin) !pending else question.canEdit
            item {
                DetailCard {
                    Text(question.title, style = MaterialTheme.typography.titleMedium)
                    Text(question.question, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        listOfNotNull(
                            question.category,
                            if (admin && !question.anonymous) question.askerName else null,
                            if (admin && !question.anonymous) (question.askerEmail ?: "N/A") else null,
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { StatusChip(question.statusLabel, contentTone(question.statusLabel)) }
                }
            }
            item {
                when {
                    admin && question.answered && question.answeredAt != null -> Text(
                        "First answered ${formatDate(question.answeredAt)} · " + when (question.approvalStatus) {
                            "APPROVED" -> "Visible on the public Fatwa section"
                            "PENDING" -> "Pending your approval"
                            else -> "Rejected"
                        },
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    !admin && question.answered -> Text(
                        "Status: " + when (question.approvalStatus) {
                            "PENDING" -> "Pending Admin Approval"
                            "REJECTED" -> "Rejected"
                            else -> "Approved and Published"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = when (question.approvalStatus) {
                            "PENDING" -> com.darsequran.academy.ui.theme.Brand.warning
                            "REJECTED" -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.primary
                        },
                    )
                    else -> {}
                }
            }
            item {
                val current = answer ?: question.answer.orEmpty()
                val label = when {
                    admin -> if (question.answered) "Update answer" else "Publish answer"
                    else -> if (question.answered) "Update draft" else "Submit draft for review"
                }
                if (editable) {
                    RichTextField(label, current, { answer = it })
                } else {
                    DetailCard(label) { HtmlText(question.answer.orEmpty()) }
                }
                Text(
                    when {
                        admin && pending -> "You are reviewing a submitted answer. It cannot be edited directly."
                        admin -> "Minimum 20 characters. The asker will be e-mailed when you save."
                        question.approvalStatus == "APPROVED" && question.answered -> "This answer is approved and published. It can no longer be edited."
                        pending -> "This draft is pending review by the admin. You cannot edit it unless it is rejected."
                        else -> "Minimum 20 characters. The admin will be notified to review it."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (!admin && !editable) com.darsequran.academy.ui.theme.Brand.warning else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (admin) item {
                FormSwitch(
                    "Show on homepage", featured ?: question.featuredOnHomepage, { featured = it },
                    "Featured in the Fatwa section when this answer is published (up to ${data.featuredMax}; ${data.featuredCount}/${data.featuredMax} slots used). All answered questions still appear on the Fatwa page.",
                )
            }
            item {
                // Pending answers are approved or rejected as they are; otherwise the edited text is sent.
                fun answerParts(withFeatured: Boolean) = formOf {
                    text("answer", if (editable) toWebsiteHtml(answer ?: question.answer.orEmpty()) else question.answer.orEmpty())
                    if (withFeatured) check("featuredOnHomepage", featured ?: question.featuredOnHomepage)
                }
                if (admin) {
                    PrimaryButton(
                        when {
                            pending -> "Approve & Publish"
                            question.answered -> "Update answer"
                            else -> "Publish answer"
                        },
                        onClick = {
                            runner.run({ service<ContentApi>().answerFatwa(id, answerParts(withFeatured = true)) }) { response ->
                                runner.message(answerPublishedMessage(response)); loader.reload()
                            }
                        },
                        loading = runner.busy,
                    )
                    if (pending) OutlinedButton(onClick = { confirmReject = true }, enabled = !runner.busy, modifier = Modifier.fillMaxWidth()) {
                        Text("Reject", color = MaterialTheme.colorScheme.error)
                    }
                } else if (editable) {
                    PrimaryButton(
                        if (question.answered) "Update draft" else "Submit draft",
                        onClick = {
                            runner.run({ service<ContentApi>().answerTeacherFatwa(id, answerParts(withFeatured = false)) }, success = "Draft saved and submitted for admin approval.") { loader.reload() }
                        },
                        loading = runner.busy,
                    )
                }
            }
            if (question.publicPath != null) item {
                OutlinedButton(onClick = { openWeb(context, question.publicPath) }) { Text("Public link: ${question.publicPath}") }
            }
            if (admin && question.anonymous) item { Banner("Anonymous question: no notification e-mail is sent to the asker.", Tone.Info) }
        }
    }

    if (confirmReject) ConfirmDialog(
        "Reject this answer?", "The teacher is told by e-mail and can revise and resubmit it.", "Reject", destructive = true,
        onConfirm = {
            val a = q?.answer.orEmpty()
            runner.run({ service<ContentApi>().rejectFatwa(id, formOf { text("answer", a) }) }, success = "Answer rejected.") { loader.reload() }
        },
        onDismiss = { confirmReject = false },
    )
    if (confirmDelete) DeleteFatwaDialog(q?.title ?: "This question", { confirmDelete = false }) {
        runner.run({ service<ContentApi>().deleteFatwa(id) }, success = "Question deleted.") { onBack() }
    }
}
