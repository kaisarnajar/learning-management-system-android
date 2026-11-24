package com.darsequran.academy.ui.manage.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.darsequran.academy.data.staff.formOf
import com.darsequran.academy.ui.common.AppCard
import com.darsequran.academy.ui.common.ArabicText
import com.darsequran.academy.ui.common.Load
import com.darsequran.academy.ui.common.ReloadOnReturn
import com.darsequran.academy.ui.common.StatusChip
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
import com.darsequran.academy.ui.manage.common.rememberActionRunner
import com.darsequran.academy.ui.manage.common.rememberLoader
import com.darsequran.academy.ui.manage.common.rememberPaged

// Website: /admin/daily-inspiration (list, delete), /[id] (view, delete), /new and /[id]/edit.

private val kinds = listOf("QURAN" to "Quranic verse", "HADITH" to "Hadith")

@Composable
private fun DeleteInspirationDialog(kindLabel: String, onDismiss: () -> Unit, onConfirm: () -> Unit) =
    ConfirmDialog("Delete this ${kindLabel.lowercase()}?", "The entry is removed for good.", "Delete", onConfirm, onDismiss, destructive = true)

@Composable
fun InspirationListScreen(onBack: () -> Unit, onOpen: (String) -> Unit, onEdit: (String) -> Unit, onNew: () -> Unit) {
    val vm = rememberPaged("admin-daily-inspiration") { page, q, _ -> service<ContentApi>().inspirations(page, q.ifBlank { null }) }
    ReloadOnReturn(vm::refresh)
    val runner = rememberActionRunner()
    var deleting by remember { mutableStateOf<InspirationDto?>(null) }

    ManageListScreen(
        title = "Verse & Hadith",
        onBack = onBack,
        vm = vm,
        key = { it.id },
        emptyTitle = "No verses or hadith posted yet",
        searchHint = "Search Arabic, translation or reference",
        snackbar = runner.snackbar,
        fab = { AddFab("New post", onNew) },
        header = {
            item {
                Text(
                    "Post a Quranic verse or Hadith for the homepage. The latest published entry is shown.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    ) { item ->
        AppCard(Modifier.fillMaxWidth(), onClick = { onOpen(item.id) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(item.kindLabel, style = MaterialTheme.typography.titleSmall)
                        StatusChip(item.statusLabel, contentTone(item.statusLabel))
                    }
                    ArabicText(if (item.arabicText.length > 60) item.arabicText.take(60) + "…" else item.arabicText, Modifier.fillMaxWidth(), size = 20)
                    Text("Updated " + formatDate(item.updatedAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                RowMenu(
                    listOf(
                        RowAction("View") { onOpen(item.id) },
                        RowAction("Edit") { onEdit(item.id) },
                        RowAction("Delete", destructive = true) { deleting = item },
                    ),
                )
            }
        }
    }
    deleting?.let { item ->
        DeleteInspirationDialog(item.kindLabel, { deleting = null }) { runner.run({ service<ContentApi>().deleteInspiration(item.id) }, success = "Entry deleted.") { vm.refresh() } }
    }
}

@Composable
fun InspirationDetailScreen(id: String, onBack: () -> Unit, onEdit: () -> Unit) {
    val loader = rememberLoader("admin-daily-inspiration-$id") { service<ContentApi>().inspiration(id).item }
    ReloadOnReturn { loader.reload() }
    val runner = rememberActionRunner()
    val state by loader.state.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    val item = (state as? Load.Ready)?.value

    ManageScaffold(item?.let { "View ${it.kindLabel}" } ?: "Verse & Hadith", onBack, runner.snackbar, actions = {
        IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, "Edit") }
        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.Delete, "Delete") }
    }) { padding ->
        LoadedColumn(loader, padding) { entry ->
            item {
                DetailCard {
                    Field("Kind", entry.kindLabel)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { StatusChip(entry.statusLabel, contentTone(entry.statusLabel)) }
                }
            }
            item { DetailCard("Arabic text") { ArabicText(entry.arabicText, Modifier.fillMaxWidth()) } }
            item {
                DetailCard("English translation") {
                    Text("“${entry.englishTranslation}”", style = MaterialTheme.typography.bodyLarge, fontStyle = FontStyle.Italic)
                }
            }
            if (!entry.reference.isNullOrBlank()) item { DetailCard("Reference") { Text(entry.reference, style = MaterialTheme.typography.bodyMedium) } }
        }
    }
    if (confirmDelete) DeleteInspirationDialog(item?.kindLabel ?: "entry", { confirmDelete = false }) {
        runner.run({ service<ContentApi>().deleteInspiration(id) }, success = "Entry deleted.") { onBack() }
    }
}

/** New ([id] null) or edit verse/hadith. */
@Composable
fun InspirationFormScreen(id: String?, onBack: () -> Unit) {
    val loader = rememberLoader("admin-daily-inspiration-form-${id ?: "new"}") { if (id != null) service<ContentApi>().inspiration(id).item else null }
    val runner = rememberActionRunner()
    val state by loader.state.collectAsStateWithLifecycle()
    val item = (state as? Load.Ready)?.value

    var loaded by rememberSaveable { mutableStateOf(id == null) }
    var kind by rememberSaveable { mutableStateOf("QURAN") }
    var arabic by rememberSaveable { mutableStateOf("") }
    var english by rememberSaveable { mutableStateOf("") }
    var reference by rememberSaveable { mutableStateOf("") }
    var published by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(item) {
        if (item != null && !loaded) {
            kind = item.kind; arabic = item.arabicText; english = item.englishTranslation; reference = item.reference.orEmpty()
            published = item.published; loaded = true
        }
    }

    FormScreen(
        title = if (id == null) "New verse or hadith" else "Edit verse or hadith",
        onBack = onBack,
        saving = runner.busy,
        snackbar = runner.snackbar,
        saveLabel = if (id == null) "Create entry" else "Save changes",
        error = (state as? Load.Failed)?.message,
        onSave = {
            val parts = formOf {
                text("kind", kind); text("arabicText", arabic.trim()); text("englishTranslation", english.trim()); text("reference", reference.trim())
                check("published", published)
            }
            runner.run(
                { if (id == null) service<ContentApi>().createInspiration(parts) else service<ContentApi>().updateInspiration(id, parts) },
                success = if (id == null) "Entry created." else "Changes saved.",
            ) { onBack() }
        },
    ) {
        if (!loaded) { Text(if (state is Load.Failed) "" else "Loading…"); return@FormScreen }
        if (id == null) Text(
            "Add Arabic text and an English translation. Publish when ready to show on the homepage.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text("Type", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            kinds.forEach { (value, label) -> FilterChip(selected = kind == value, onClick = { kind = value }, label = { Text(label) }) }
        }
        // Arabic is typed right to left; the preview below shows it as the website sets it.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            OutlinedTextField(
                arabic, { arabic = it.take(5000) },
                label = { Text("Arabic text *") },
                placeholder = { Text("النص العربي…") },
                minLines = 4,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (arabic.isNotBlank()) DetailCard("Preview") { ArabicText(arabic, Modifier.fillMaxWidth(), size = 22) }
        FormTextField("English translation *", english, { english = it }, singleLine = false, minLines = 3, maxLength = 5000, supporting = "English meaning or translation")
        FormTextField("Reference (optional)", reference, { reference = it }, maxLength = 300, supporting = "e.g. Surah Al-Baqarah 2:255 or Sahih al-Bukhari 1")
        FormSwitch("Publish on homepage", published, { published = it }, "The latest published entry is shown on the homepage.")
    }
}
