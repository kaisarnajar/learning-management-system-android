package com.darsequran.academy.ui.manage.people

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.darsequran.academy.data.staff.formOf
import com.darsequran.academy.ui.common.Avatar
import com.darsequran.academy.ui.common.Load
import com.darsequran.academy.ui.common.ReloadOnReturn
import com.darsequran.academy.ui.common.StatusChip
import com.darsequran.academy.ui.common.Tone
import com.darsequran.academy.ui.common.countOf
import com.darsequran.academy.ui.common.formatDate
import com.darsequran.academy.ui.manage.common.AddFab
import com.darsequran.academy.ui.manage.common.ConfirmDialog
import com.darsequran.academy.ui.manage.common.DetailCard
import com.darsequran.academy.ui.manage.common.Field
import com.darsequran.academy.ui.manage.common.FormScreen
import com.darsequran.academy.ui.manage.common.FormSwitch
import com.darsequran.academy.ui.manage.common.FormTextField
import com.darsequran.academy.ui.manage.common.LoadedColumn
import com.darsequran.academy.ui.manage.common.ManageListScreen
import com.darsequran.academy.ui.manage.common.ManageScaffold
import com.darsequran.academy.ui.manage.common.Notice
import com.darsequran.academy.ui.manage.common.RecordCard
import com.darsequran.academy.ui.manage.common.rememberActionRunner
import com.darsequran.academy.ui.manage.common.rememberContainer
import com.darsequran.academy.ui.manage.common.rememberLoader
import com.darsequran.academy.ui.manage.common.rememberPaged
import com.darsequran.academy.data.apiCall
import com.darsequran.academy.data.ApiResult
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

private fun publishedChip(published: Boolean) = if (published) "Published" to Tone.Success else "Draft" to Tone.Neutral

/** Two initials from a name, as the website derives them ("Abdul Rahman" → "AR"). */
private fun initialsOf(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "??"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> "${parts.first()[0]}${parts.last()[0]}".uppercase()
    }
}

@Composable
fun TeachersListScreen(onBack: () -> Unit, onOpen: (String) -> Unit, onAdd: () -> Unit) {
    val vm = rememberPaged("admin-teachers") { page, q, _ -> service<TeachersAdminApi>().list(page, q.ifBlank { null }) }
    ReloadOnReturn(vm::refresh)
    ManageListScreen(
        title = "Teachers",
        onBack = onBack,
        vm = vm,
        key = { it.id },
        emptyTitle = "No teachers yet",
        searchHint = "Name, e-mail or specialization",
        fab = { AddFab("Add teacher", onAdd) },
    ) { t ->
        RecordCard(
            title = t.name,
            subtitle = listOfNotNull(t.specialization.ifBlank { null }, t.email).joinToString(" · "),
            meta = t.createdAt?.let { "Added " + formatDate(it) },
            chip = publishedChip(t.published),
            leading = { Avatar(t.name, t.imageUrl) },
            onClick = { onOpen(t.id) },
        )
    }
}

@Composable
fun TeacherDetailScreen(id: String, onBack: () -> Unit, onEdit: () -> Unit) {
    val loader = rememberLoader("admin-teacher-$id") { service<TeachersAdminApi>().get(id).teacher }
    ReloadOnReturn { loader.reload() }
    val runner = rememberActionRunner()
    val state by loader.state.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    ManageScaffold("Teacher profile", onBack, runner.snackbar, actions = {
        IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, "Edit teacher") }
        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.Delete, "Delete teacher") }
    }) { padding ->
        LoadedColumn(loader, padding) { t ->
            item {
                DetailCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(t.name, t.imageUrl, 72.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(t.name, style = MaterialTheme.typography.titleLarge)
                            Text(t.email ?: "No login email linked", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(4.dp))
                            publishedChip(t.published).let { StatusChip(it.first, it.second) }
                        }
                    }
                }
            }
            item {
                DetailCard("Profile") {
                    Field("Specialization", t.specialization)
                    Field("Initials", t.initials)
                    Field("Bio", t.bio)
                    Field("Registered", t.createdAt?.let(::formatDate))
                    Field("Profile ID", t.id)
                }
            }
            item {
                DetailCard("Courses teaching") {
                    if (t.courses.isEmpty()) Text("Not assigned to any courses yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    t.courses.forEach { c ->
                        Column {
                            Text(c.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                listOfNotNull(c.category, countOf(c.studentCount, "student")).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            StatusChip(c.statusLabel.ifBlank { c.status }, courseTone(c.status))
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        val t = (state as? Load.Ready)?.value
        val n = t?.courses?.size ?: 0
        ConfirmDialog(
            "Delete ${t?.name ?: "this teacher"}?",
            if (n > 0) "${t?.name} is assigned to ${countOf(n, "course")}. Reassign ${if (n == 1) "it" else "them"} to another teacher before deleting."
            else "The teacher profile is removed from the site. Their user account stays.",
            "Delete", destructive = true,
            onConfirm = { runner.run({ service<TeachersAdminApi>().delete(id) }, success = "Teacher deleted.") { onBack() } },
            onDismiss = { confirmDelete = false },
        )
    }
}

private fun courseTone(status: String) = when (status) {
    "ONGOING" -> Tone.Success
    "PUBLISHED", "COMING_SOON" -> Tone.Info
    "ON_HOLD" -> Tone.Warning
    else -> Tone.Neutral
}

/** Add ([id] null) or edit a teacher. The e-mail must belong to a registered account; its name is used. */
@Composable
fun TeacherFormScreen(id: String?, onBack: () -> Unit) {
    val runner = rememberActionRunner()
    val container = rememberContainer()
    val scope = rememberCoroutineScope()
    val loader = if (id != null) rememberLoader("admin-teacher-$id") { service<TeachersAdminApi>().get(id).teacher } else null
    val existing = loader?.state?.collectAsStateWithLifecycle()?.value?.let { (it as? Load.Ready)?.value }

    var loaded by rememberSaveable { mutableStateOf(id == null) }
    var email by rememberSaveable { mutableStateOf("") }
    var linkedName by rememberSaveable { mutableStateOf("") }
    var lookupError by rememberSaveable { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }
    var checkedEmail by rememberSaveable { mutableStateOf("") }
    var specialization by rememberSaveable { mutableStateOf("") }
    var bio by rememberSaveable { mutableStateOf("") }
    var initials by rememberSaveable { mutableStateOf("") }
    var published by rememberSaveable { mutableStateOf(true) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(existing) {
        if (existing != null && !loaded) {
            email = existing.email.orEmpty(); linkedName = existing.name; checkedEmail = email
            specialization = existing.specialization; bio = existing.bio; initials = existing.initials; published = existing.published
            loaded = true
        }
    }

    fun verify() {
        val target = email.trim()
        if (target.isEmpty()) { linkedName = ""; lookupError = null; return }
        checking = true
        scope.launch {
            when (val r = apiCall { container.service<TeachersAdminApi>().lookup(target, id).lookup }) {
                is ApiResult.Ok -> if (r.value.ok) {
                    linkedName = r.value.name.orEmpty(); lookupError = null; checkedEmail = target
                    if (initials.isBlank() || id == null) initials = initialsOf(linkedName)
                } else { linkedName = ""; lookupError = r.value.error ?: "This e-mail cannot be used." }
                is ApiResult.Err -> { linkedName = ""; lookupError = r.message }
            }
            checking = false
        }
    }

    val verified = linkedName.isNotBlank() && lookupError == null && checkedEmail.equals(email.trim(), ignoreCase = true)

    FormScreen(
        title = if (id == null) "Add teacher" else "Edit teacher",
        onBack = onBack,
        saving = runner.busy,
        snackbar = runner.snackbar,
        saveLabel = if (id == null) "Create teacher" else "Save changes",
        error = error,
        onSave = {
            error = when {
                !verified -> "Verify the account e-mail first."
                specialization.isBlank() -> "Enter a specialization."
                bio.isBlank() -> "Enter a bio."
                else -> null
            }
            if (error == null) {
                val form = formOf {
                    text("email", email.trim()); text("specialization", specialization.trim()); text("bio", bio.trim())
                    text("initials", initials.trim()); check("published", published)
                }
                runner.run(
                    { if (id == null) service<TeachersAdminApi>().create(form) else service<TeachersAdminApi>().update(id, form) },
                    success = if (id == null) "Teacher created." else "Changes saved.",
                ) { onBack() }
            }
        },
    ) {
        if (!loaded) { Text("Loading…"); return@FormScreen }
        if (id == null) Text(
            "Link an existing site account by e-mail. The teacher must register first; you assign specialization and public profile details.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FormTextField(
            "Registered account email", email, { email = it; if (!it.trim().equals(checkedEmail, true)) linkedName = "" },
            keyboardType = KeyboardType.Email,
            supporting = "The teacher must already have signed up on the site. We use their account name automatically.",
        )
        OutlinedButton(onClick = ::verify, enabled = !checking && email.isNotBlank()) { Text(if (checking) "Checking…" else "Verify account") }
        Notice(lookupError, Tone.Warning)
        Field("Name (from account)", linkedName.ifBlank { "Verify the email above to load the account name." })
        FormTextField("Specialization", specialization, { specialization = it }, supporting = "e.g. Quran & Tajweed")
        FormTextField("Bio", bio, { bio = it }, singleLine = false, minLines = 4)
        FormTextField(
            "Initials (avatar)", initials, { initials = it.uppercase() }, maxLength = 4,
            capitalization = KeyboardCapitalization.Characters,
            supporting = "Leave blank to auto-generate from the account name.",
        )
        FormSwitch("Published on public Teachers page", published, { published = it })
    }
}
