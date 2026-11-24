package com.darsequran.academy.ui.manage.people

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.darsequran.academy.data.staff.ActionResponse
import com.darsequran.academy.data.staff.formOf
import com.darsequran.academy.ui.common.Load
import com.darsequran.academy.ui.common.ReloadOnReturn
import com.darsequran.academy.ui.common.Tone
import com.darsequran.academy.ui.common.formatDate
import com.darsequran.academy.ui.manage.common.ConfirmDialog
import com.darsequran.academy.ui.manage.common.DetailCard
import com.darsequran.academy.ui.manage.common.Field
import com.darsequran.academy.ui.manage.common.FilterOption
import com.darsequran.academy.ui.manage.common.FormScreen
import com.darsequran.academy.ui.manage.common.FormTextField
import com.darsequran.academy.ui.manage.common.LoadedColumn
import com.darsequran.academy.ui.manage.common.ManageListScreen
import com.darsequran.academy.ui.manage.common.ManageScaffold
import com.darsequran.academy.ui.manage.common.rememberActionRunner
import com.darsequran.academy.ui.manage.common.rememberLoader
import com.darsequran.academy.ui.manage.common.rememberPaged

private fun inquiryChip(i: ContactInquiry) = (i.statusLabel.ifBlank { if (i.reply != null) "Replied" else "Pending" }) to (if (i.reply != null) Tone.Success else Tone.Warning)

private fun repliedLine(i: ContactInquiry) =
    i.repliedAt?.let { formatDate(it) + (i.repliedBy?.name?.let { n -> " by $n" } ?: "") }

/** What the website's toast says after a reply, by whether the e-mail went out. */
private fun replySavedMessage(r: ActionResponse): String {
    val email = r.redirect?.substringAfter("email=", "")?.substringBefore('&').orEmpty()
    return "Reply saved." + when (email) {
        "failed" -> " The notification email could not be sent — check SMTP settings in your environment."
        "skipped" -> " SMTP is not configured, so no email was sent."
        else -> " The visitor has been notified by email. If they don't see it, ask them to check their Spam/Junk folder."
    }
}

@Composable
fun InquiriesListScreen(onBack: () -> Unit, onOpen: (String) -> Unit, onReply: (String) -> Unit) {
    val vm = rememberPaged("admin-inquiries") { page, q, filter -> service<ContactInquiriesApi>().list(page, q.ifBlank { null }, filter) }
    ReloadOnReturn(vm::refresh)
    val runner = rememberActionRunner()
    var deleting by remember { mutableStateOf<ContactInquiry?>(null) }
    ManageListScreen(
        title = "Contact inquiries",
        onBack = onBack,
        vm = vm,
        key = { it.id },
        emptyTitle = "No contact messages yet",
        searchHint = "Name, e-mail, phone or message",
        filters = listOf(FilterOption(null, "All"), FilterOption("pending", "Pending"), FilterOption("replied", "Replied")),
        snackbar = runner.snackbar,
    ) { i ->
        ActionCard(
            title = i.name,
            subtitle = listOf(i.email, i.phone).filter { it.isNotBlank() }.joinToString(" · "),
            meta = "Submitted " + formatDate(i.createdAt),
            body = i.message,
            chip = inquiryChip(i),
            onClick = { onOpen(i.id) },
        ) {
            SmallOutlined(if (i.reply != null) "Edit reply" else "Reply", { onReply(i.id) })
            SmallDanger("Delete", { deleting = i }, enabled = !runner.busy)
        }
    }
    deleting?.let { i ->
        ConfirmDialog(
            "Delete this inquiry?", "${i.name} (${i.email}) — the message is removed permanently.", "Delete", destructive = true,
            onConfirm = { runner.run({ service<ContactInquiriesApi>().delete(i.id) }, success = "Inquiry deleted.") { vm.refresh() } },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
fun InquiryDetailScreen(id: String, onBack: () -> Unit, onReply: () -> Unit) {
    val loader = rememberLoader("admin-inquiry-$id") { service<ContactInquiriesApi>().get(id).inquiry }
    ReloadOnReturn { loader.reload() }
    val runner = rememberActionRunner()
    var confirmDelete by remember { mutableStateOf(false) }
    ManageScaffold("Contact inquiry", onBack, runner.snackbar, actions = {
        IconButton(onClick = onReply) { Icon(Icons.AutoMirrored.Rounded.Reply, "Reply") }
        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.Delete, "Delete inquiry") }
    }) { padding ->
        LoadedColumn(loader, padding) { i ->
            item {
                DetailCard {
                    Field("Name", i.name)
                    Field("Email", i.email)
                    Field("Phone", i.phone)
                    inquiryChip(i).let { com.darsequran.academy.ui.common.StatusChip(it.first, it.second) }
                    Field("Message", i.message)
                    Field("Reply", i.reply, showEmpty = false)
                    Field("Replied", repliedLine(i), showEmpty = false)
                    Field("Submitted", formatDate(i.createdAt))
                }
            }
            item { SmallPrimary(if (i.reply != null) "Edit reply" else "Reply", onReply) }
        }
    }
    if (confirmDelete) ConfirmDialog(
        "Delete this inquiry?", "The message is removed permanently.", "Delete", destructive = true,
        onConfirm = { runner.run({ service<ContactInquiriesApi>().delete(id) }, success = "Inquiry deleted.") { onBack() } },
        onDismiss = { confirmDelete = false },
    )
}

/** /admin/contact-inquiries/[id]/reply: the reply is saved and e-mailed to the visitor. */
@Composable
fun InquiryReplyScreen(id: String, onBack: () -> Unit) {
    val loader = rememberLoader("admin-inquiry-$id") { service<ContactInquiriesApi>().get(id).inquiry }
    val runner = rememberActionRunner()
    val state by loader.state.collectAsStateWithLifecycle()
    val inquiry = (state as? Load.Ready)?.value
    var reply by rememberSaveable { mutableStateOf("") }
    var loaded by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    LaunchedEffect(inquiry) { if (inquiry != null && !loaded) { reply = inquiry.reply.orEmpty(); loaded = true } }
    val replied = inquiry?.reply != null

    FormScreen(
        title = if (replied) "Edit reply" else "Reply to inquiry",
        onBack = onBack,
        saving = runner.busy,
        snackbar = runner.snackbar,
        saveLabel = if (replied) "Update reply & resend email" else "Send reply",
        error = error,
        onSave = {
            error = if (reply.trim().length < 10) "The reply must be at least 10 characters." else null
            if (error == null) runner.run({ service<ContactInquiriesApi>().reply(id, formOf { text("reply", reply.trim()) }) }) { r ->
                runner.message(replySavedMessage(r)); loader.reload()
            }
        },
    ) {
        when (val s = state) {
            is Load.Failed -> { com.darsequran.academy.ui.manage.common.Notice(s.message); return@FormScreen }
            Load.Loading -> { Text("Loading…"); return@FormScreen }
            is Load.Ready -> Unit
        }
        val i = inquiry ?: return@FormScreen
        repliedLine(i)?.let { Text((if (replied) "Replied " else "Last updated ") + it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        DetailCard {
            Text(i.name, style = MaterialTheme.typography.titleSmall)
            Text("${i.email} · ${i.phone}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(i.message, style = MaterialTheme.typography.bodyMedium)
        }
        FormTextField(
            if (replied) "Update reply" else "Reply to visitor", reply, { reply = it }, singleLine = false, minLines = 8, maxLength = 10000,
            supporting = "Minimum 10 characters. The visitor will receive this reply by email.",
        )
        SmallDanger("Delete inquiry", { confirmDelete = true }, enabled = !runner.busy)
    }
    if (confirmDelete) ConfirmDialog(
        "Delete this inquiry?", "The message is removed permanently.", "Delete", destructive = true,
        onConfirm = { runner.run({ service<ContactInquiriesApi>().delete(id) }, success = "Inquiry deleted.") { onBack() } },
        onDismiss = { confirmDelete = false },
    )
}
