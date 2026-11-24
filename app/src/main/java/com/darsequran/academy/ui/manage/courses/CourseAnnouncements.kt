package com.darsequran.academy.ui.manage.courses

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.darsequran.academy.data.staff.courses.CourseAnnouncement
import com.darsequran.academy.data.staff.courses.CourseToolsApi
import com.darsequran.academy.data.staff.courses.Portal
import com.darsequran.academy.data.staff.courses.announcementScope
import com.darsequran.academy.data.staff.formOf
import com.darsequran.academy.ui.common.AppCard
import com.darsequran.academy.ui.common.Load
import com.darsequran.academy.ui.common.ReloadOnReturn
import com.darsequran.academy.ui.common.StatusChip
import com.darsequran.academy.ui.common.Tone
import com.darsequran.academy.ui.common.downloadAndOpen
import com.darsequran.academy.ui.common.formatDateTime
import com.darsequran.academy.ui.manage.common.AddFab
import com.darsequran.academy.ui.manage.common.ConfirmDialog
import com.darsequran.academy.ui.manage.common.FormCheckbox
import com.darsequran.academy.ui.manage.common.FormDropdown
import com.darsequran.academy.ui.manage.common.FormFileField
import com.darsequran.academy.ui.manage.common.FormScreen
import com.darsequran.academy.ui.manage.common.FormTextField
import com.darsequran.academy.ui.manage.common.ManageListScreen
import com.darsequran.academy.ui.manage.common.Notice
import com.darsequran.academy.ui.manage.common.rememberActionRunner
import com.darsequran.academy.ui.manage.common.rememberFormFiles
import com.darsequran.academy.ui.manage.common.rememberLoader
import com.darsequran.academy.ui.manage.common.rememberPaged
import com.darsequran.academy.ui.theme.Brand
import kotlinx.coroutines.launch

/*
 * Course announcements (website: …/courses/[id]/announcements, /new, /[announcementId]/edit) for admins and
 * teachers, and a teacher's private messages to one student (…/students/[enrollmentId]/announcements) when
 * `enrollmentId` is set.
 */

/** File types the website accepts as an announcement attachment. */
private val ATTACHMENT_TYPES = arrayOf(
    "application/pdf", "image/jpeg", "image/png", "image/webp", "image/gif",
    "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
)

private val DEFAULT_CATEGORIES = listOf(
    "EXAMS_TESTS" to "Exams & Tests",
    "ASSIGNMENTS_HOMEWORK" to "Assignments & Homework",
    "STUDY_MATERIALS" to "Study Materials",
    "CLASS_SCHEDULE" to "Class Schedule",
    "COURSE_ANNOUNCEMENT" to "Course Announcements",
    "GENERAL_NOTICE" to "General Notices",
)

private fun categoryTone(category: String) = when (category) {
    "EXAMS_TESTS" -> Tone.Danger
    "ASSIGNMENTS_HOMEWORK" -> Tone.Warning
    "STUDY_MATERIALS", "CLASS_SCHEDULE" -> Tone.Info
    "COURSE_ANNOUNCEMENT" -> Tone.Success
    else -> Tone.Neutral
}

@Composable
fun CourseAnnouncementsScreen(
    portal: Portal,
    courseId: String,
    enrollmentId: String?,
    studentName: String?,
    onBack: () -> Unit,
    onNew: () -> Unit,
    onEdit: (String) -> Unit,
) {
    val personal = enrollmentId != null
    val vm = rememberPaged("course-announcements-${portal.path}-$courseId-${enrollmentId ?: "all"}") { page, q, _ ->
        if (enrollmentId != null) service<CourseToolsApi>().messages(portal.path, courseId, enrollmentId, page)
        else service<CourseToolsApi>().announcements(portal.path, courseId, page, q.ifBlank { null })
    }
    ReloadOnReturn(vm::refresh)
    val runner = rememberActionRunner()
    var deleting by remember { mutableStateOf<CourseAnnouncement?>(null) }
    val who = studentName ?: "this student"

    ManageListScreen(
        title = if (personal) "Messages" else "Announcements",
        onBack = onBack,
        vm = vm,
        key = { it.id },
        emptyTitle = if (personal) "No private messages for $who yet" else "No announcements for this course yet",
        searchHint = if (personal) null else "Search by title, body, or author",
        snackbar = runner.snackbar,
        fab = { AddFab(if (personal) "New message" else "New announcement", onNew) },
        header = {
            item {
                Text(
                    when {
                        personal -> "Private announcements for $who only: exam marks, feedback and personal updates."
                        portal == Portal.Teacher -> "Course-wide updates for all enrolled students. To message one student (e.g. exam marks), use Message on the student's page."
                        else -> "Post course-wide updates for enrolled students: schedule changes, academy notices, and more."
                    },
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    ) { a ->
        AnnouncementCard(a, if (personal) "For $who" else null, runner.snackbar, onEdit = { onEdit(a.id) }, onDelete = { deleting = a })
    }

    deleting?.let { a ->
        ConfirmDialog(
            if (personal) "Delete this message?" else "Delete this announcement?",
            "\"${a.title}\" will be removed for students too. This cannot be undone.",
            "Delete", destructive = true,
            onConfirm = {
                runner.run(
                    { service<CourseToolsApi>().deleteAnnouncement(portal.path, courseId, announcementScope(enrollmentId), a.id) },
                    success = if (personal) "Message deleted." else "Announcement deleted.",
                ) { vm.refresh() }
            },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun AnnouncementCard(a: CourseAnnouncement, audience: String?, snackbar: SnackbarHostState, onEdit: () -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    AppCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusChip(a.categoryLabel.ifBlank { a.category }, categoryTone(a.category))
            Spacer(Modifier.weight(1f))
            if (a.canManage) {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) { Icon(Icons.Rounded.Edit, "Edit") }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) { Icon(Icons.Rounded.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(a.title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(a.body, style = MaterialTheme.typography.bodyMedium)
        if (a.attachmentPath != null) {
            Spacer(Modifier.height(8.dp))
            AssistChip(
                onClick = {
                    scope.launch {
                        snackbar.showSnackbar("Opening ${a.attachmentName ?: "attachment"}…")
                        downloadAndOpen(context, a.attachmentPath, a.attachmentName ?: "attachment")?.let { snackbar.showSnackbar(it) }
                    }
                },
                label = { Text(a.attachmentName ?: "Attachment") },
                leadingIcon = { Icon(Icons.Rounded.AttachFile, null, Modifier.size(18.dp)) },
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            listOfNotNull(a.authorName, formatDateTime(a.createdAt), "Edited".takeIf { a.edited }, audience).joinToString(" · "),
            style = MaterialTheme.typography.labelSmall, color = Brand.gold,
        )
        if (!a.canManage && a.postedByAdmin) {
            Text("Posted by the academy administration", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun AnnouncementFormScreen(
    portal: Portal,
    courseId: String,
    enrollmentId: String?,
    announcementId: String?,
    studentName: String?,
    onBack: () -> Unit,
) {
    val personal = enrollmentId != null
    val scope = announcementScope(enrollmentId)
    val runner = rememberActionRunner()
    val loader = if (announcementId != null) rememberLoader("announcement-${portal.path}-$courseId-$announcementId") {
        service<CourseToolsApi>().announcement(portal.path, courseId, scope, announcementId)
    } else null
    val state = loader?.state?.collectAsStateWithLifecycle()?.value
    val existing = (state as? Load.Ready)?.value?.announcement

    var loaded by rememberSaveable { mutableStateOf(announcementId == null) }
    var category by rememberSaveable { mutableStateOf(if (personal) "EXAMS_TESTS" else "COURSE_ANNOUNCEMENT") }
    var title by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    var removeAttachment by rememberSaveable { mutableStateOf(false) }
    var file by rememberFormFiles("course-announcement-files-${portal.path}-$courseId-${announcementId ?: enrollmentId ?: "new"}")::single
    LaunchedEffect(existing) {
        if (existing != null && !loaded) { category = existing.category; title = existing.title; body = existing.body; loaded = true }
    }
    val categories = (state as? Load.Ready)?.value?.categories?.map { it.value to it.label }?.ifEmpty { null } ?: DEFAULT_CATEGORIES
    val error = when {
        title.isNotEmpty() && title.trim().length < 3 -> "Title must be at least 3 characters."
        body.isNotEmpty() && body.trim().length < 10 -> "Message must be at least 10 characters."
        else -> null
    }

    FormScreen(
        title = when {
            announcementId != null && personal -> "Edit message"
            announcementId != null -> "Edit announcement"
            personal -> "Message for ${studentName ?: "student"}"
            else -> "New announcement"
        },
        onBack = onBack,
        saving = runner.busy,
        snackbar = runner.snackbar,
        saveLabel = when {
            announcementId != null -> "Save changes"
            personal -> "Send message"
            else -> "Post announcement"
        },
        canSave = loaded,
        loadError = (state as? Load.Failed)?.message,
        onRetry = { loader?.reload(true) },
        onSave = {
            if (title.trim().length < 3 || body.trim().length < 10) { runner.message(error ?: "Add a title and a message."); return@FormScreen }
            val parts = formOf {
                text("category", category); text("title", title.trim()); text("body", body.trim())
                file("attachment", file)
                if (removeAttachment && file == null) text("removeAttachment", "1")
            }
            runner.run(
                {
                    if (announcementId == null) service<CourseToolsApi>().createAnnouncement(portal.path, courseId, scope, parts)
                    else service<CourseToolsApi>().updateAnnouncement(portal.path, courseId, scope, announcementId, parts)
                },
                success = when {
                    announcementId != null -> if (personal) "Message updated." else "Announcement updated."
                    personal -> "Message sent to ${studentName ?: "the student"}. They will be notified."
                    else -> "Announcement posted. Enrolled students will be notified."
                },
            ) { onBack() }
        },
    ) {
        if (announcementId != null && existing == null) { if (state !is Load.Failed) Text("Loading…"); return@FormScreen }
        Text(
            when {
                personal -> "Only ${studentName ?: "this student"} will see this message (e.g. exam score, assignment feedback)."
                else -> "Students enrolled in this course will see this announcement."
            },
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FormDropdown("Category", categories, category, { category = it })
        FormTextField("Title", title, { title = it }, maxLength = 200, supporting = "e.g. Mid-term exam schedule", isError = title.isNotEmpty() && title.trim().length < 3)
        FormTextField("Message", body, { body = it }, singleLine = false, minLines = 8, maxLength = 10000, isError = body.isNotEmpty() && body.trim().length < 10)
        FormFileField(
            "Attachment (optional)", ATTACHMENT_TYPES, file, { file = it },
            currentName = existing?.attachmentName?.takeIf { !removeAttachment }?.let { "Current: $it" },
            supporting = if (file != null && existing?.attachmentName != null) "The new file replaces the current attachment."
            else "PDF, image or Word document.",
        )
        if (existing?.attachmentPath != null && file == null) FormCheckbox("Remove the current attachment", removeAttachment) { removeAttachment = it }
        if (error != null) Notice(error, Tone.Warning)
        Spacer(Modifier.width(1.dp))
    }
}
