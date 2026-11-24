package com.darsequran.academy.ui.manage.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.PictureAsPdf
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.darsequran.academy.data.staff.formOf
import com.darsequran.academy.ui.common.Load
import com.darsequran.academy.ui.common.ReloadOnReturn
import com.darsequran.academy.ui.common.RemoteImage
import com.darsequran.academy.ui.common.StatusChip
import com.darsequran.academy.ui.common.Tone
import com.darsequran.academy.ui.common.formatDate
import com.darsequran.academy.ui.common.openWeb
import com.darsequran.academy.ui.manage.common.AddFab
import com.darsequran.academy.ui.manage.common.ConfirmDialog
import com.darsequran.academy.ui.manage.common.DetailCard
import com.darsequran.academy.ui.manage.common.Field
import com.darsequran.academy.ui.manage.common.FormDropdown
import com.darsequran.academy.ui.manage.common.FormImageField
import com.darsequran.academy.ui.manage.common.FormScreen
import com.darsequran.academy.ui.manage.common.FormSection
import com.darsequran.academy.ui.manage.common.FormSwitch
import com.darsequran.academy.ui.manage.common.FormTextField
import com.darsequran.academy.ui.manage.common.LoadedColumn
import com.darsequran.academy.ui.manage.common.ManageListScreen
import com.darsequran.academy.ui.manage.common.ManageScaffold
import com.darsequran.academy.ui.manage.common.RecordCard
import com.darsequran.academy.ui.manage.common.rememberActionRunner
import com.darsequran.academy.ui.manage.common.rememberLoader
import com.darsequran.academy.ui.manage.common.rememberPaged
import com.darsequran.academy.ui.manage.common.rememberFormFiles
import com.darsequran.academy.ui.manage.common.FeaturedSwitch

// Website: /admin/library (list, delete), /[id] (view, delete), /new and /[id]/edit (cover image upload).

private fun libraryTone(item: LibraryItemDto) = if (item.published) Tone.Info else Tone.Neutral

@Composable
private fun Cover(path: String?, width: Int = 36, height: Int = 48) {
    if (path != null) RemoteImage(path, null, Modifier.size(width.dp, height.dp).clip(RoundedCornerShape(6.dp)))
    else com.darsequran.academy.ui.manage.common.RowIcon(Icons.AutoMirrored.Rounded.MenuBook)
}

@Composable
private fun DeleteLibraryDialog(title: String, onDismiss: () -> Unit, onConfirm: () -> Unit) =
    ConfirmDialog("Delete this item?", "\"$title\" and its cover are removed from the library for good.", "Delete", onConfirm, onDismiss, destructive = true)

@Composable
fun LibraryListScreen(onBack: () -> Unit, onOpen: (String) -> Unit, onEdit: (String) -> Unit, onNew: () -> Unit) {
    val vm = rememberPaged("admin-library") { page, q, _ -> service<ContentApi>().library(page, q.ifBlank { null }) }
    ReloadOnReturn(vm::refresh)
    val runner = rememberActionRunner()
    var deleting by remember { mutableStateOf<LibraryItemDto?>(null) }

    ManageListScreen(
        title = "Library",
        onBack = onBack,
        vm = vm,
        key = { it.id },
        emptyTitle = "No library items yet",
        searchHint = "Search by title, author or topic",
        snackbar = runner.snackbar,
        fab = { AddFab("Add item", onNew) },
    ) { item ->
        RecordCard(
            title = item.title,
            subtitle = "${item.author} · ${item.topic}",
            meta = listOf(item.level, item.language, if (item.featuredOnHomepage) "Featured" else "Not featured").joinToString(" · "),
            chip = item.statusLabel to libraryTone(item),
            leading = { Cover(item.imagePath) },
            trailing = {
                RowMenu(
                    listOf(
                        RowAction("View") { onOpen(item.id) },
                        RowAction("Edit") { onEdit(item.id) },
                        RowAction("Delete", destructive = true) { deleting = item },
                    ),
                )
            },
            onClick = { onOpen(item.id) },
        )
    }
    deleting?.let { item ->
        DeleteLibraryDialog(item.title, { deleting = null }) { runner.run({ service<ContentApi>().deleteLibraryItem(item.id) }, success = "Item deleted.") { vm.refresh() } }
    }
}

@Composable
fun LibraryDetailScreen(id: String, onBack: () -> Unit, onEdit: () -> Unit) {
    val loader = rememberLoader("admin-library-$id") { service<ContentApi>().libraryItem(id).item }
    ReloadOnReturn { loader.reload() }
    val runner = rememberActionRunner()
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }
    val state by loader.state.collectAsStateWithLifecycle()

    ManageScaffold("Library item", onBack, runner.snackbar, actions = {
        IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, "Edit item") }
        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.Delete, "Delete item") }
    }) { padding ->
        LoadedColumn(loader, padding) { item ->
            item {
                DetailCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Cover(item.imagePath, 96, 128)
                        Spacer(Modifier.width(14.dp))
                        androidx.compose.foundation.layout.Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(item.title, style = MaterialTheme.typography.titleLarge)
                            Text("${item.author} · ${item.topic}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            StatusChip(item.statusLabel, libraryTone(item))
                        }
                    }
                }
            }
            item {
                DetailCard {
                    Field("Title", item.title)
                    Field("Author", item.author)
                    Field("Topic", item.topic)
                    Field("Level", item.level)
                    Field("Language", item.language)
                    Field("Homepage", if (item.featuredOnHomepage) "Featured" else "Not featured")
                    Field("PDF", item.pdfUrl)
                    if (!item.pdfUrl.isNullOrBlank()) OutlinedButton(onClick = { openWeb(context, item.pdfUrl) }) {
                        Icon(Icons.Rounded.PictureAsPdf, null); Text("  Open PDF")
                    }
                    Field("Added", formatDate(item.createdAt))
                    Field("ID", item.id)
                }
            }
        }
    }
    if (confirmDelete) DeleteLibraryDialog((state as? Load.Ready)?.value?.title ?: "This item", { confirmDelete = false }) {
        runner.run({ service<ContentApi>().deleteLibraryItem(id) }, success = "Item deleted.") { onBack() }
    }
}

/** A choice from [options] where "Other" lets the admin type their own value (the website form's topic/language). */
@Composable
private fun ChoiceWithOther(label: String, options: List<String>, value: String, onChange: (String) -> Unit, custom: Boolean, onCustomChange: (Boolean) -> Unit) {
    val choices = (if (value.isNotBlank() && !custom && value !in options) listOf(value) else emptyList()) + options
    FormDropdown(label, choices.map { it to it }, if (custom) "Other" else value, { picked ->
        if (picked == "Other") { onCustomChange(true); onChange("") } else { onCustomChange(false); onChange(picked) }
    }, placeholder = "Select…")
    if (custom) FormTextField("Specify ${label.lowercase()}", value, onChange, maxLength = 100)
}

/** New ([id] null) or edit library item. */
@Composable
fun LibraryFormScreen(id: String?, onBack: () -> Unit) {
    val loader = rememberLoader("admin-library-form-${id ?: "new"}") {
        val api = service<ContentApi>()
        if (id != null) api.libraryItem(id).let { it.item to it.options } else null to api.libraryOptions()
    }
    val runner = rememberActionRunner()
    val state by loader.state.collectAsStateWithLifecycle()
    val data = (state as? Load.Ready)?.value
    val item = data?.first
    val options = data?.second

    var loaded by rememberSaveable { mutableStateOf(id == null) }
    var title by rememberSaveable { mutableStateOf("") }
    var author by rememberSaveable { mutableStateOf("") }
    var topic by rememberSaveable { mutableStateOf("") }
    var customTopic by rememberSaveable { mutableStateOf(false) }
    var level by rememberSaveable { mutableStateOf("Beginner") }
    var language by rememberSaveable { mutableStateOf("") }
    var customLanguage by rememberSaveable { mutableStateOf(false) }
    var pdfUrl by rememberSaveable { mutableStateOf("") }
    var published by rememberSaveable { mutableStateOf(true) }
    var featured by rememberSaveable { mutableStateOf(false) }
    var removeCover by rememberSaveable { mutableStateOf(false) }
    var cover by rememberFormFiles("library-files-${id ?: "new"}")::single
    LaunchedEffect(item) {
        if (item != null && !loaded) {
            title = item.title; author = item.author; topic = item.topic; level = item.level; language = item.language
            pdfUrl = item.pdfUrl.orEmpty(); published = item.published; featured = item.featuredOnHomepage; loaded = true
        }
    }

    FormScreen(
        title = if (id == null) "Add library item" else "Edit library item",
        onBack = onBack,
        saving = runner.busy,
        snackbar = runner.snackbar,
        saveLabel = if (id == null) "Create item" else "Save changes",
        canSave = loaded && state is Load.Ready,
        loadError = (state as? Load.Failed)?.message,
        onRetry = { loader.reload(true) },
        onSave = {
            val parts = formOf {
                text("title", title.trim()); text("author", author.trim()); text("topic", topic.trim()); text("level", level)
                text("language", language.trim()); text("pdfUrl", pdfUrl.trim())
                check("published", published); check("featuredOnHomepage", featured)
                file("image", cover)
                if (id != null) text("removeImage", if (removeCover && cover == null) "true" else "false")
            }
            runner.run(
                { if (id == null) service<ContentApi>().createLibraryItem(parts) else service<ContentApi>().updateLibraryItem(id, parts) },
                success = if (id == null) "Item created." else "Changes saved.",
            ) { onBack() }
        },
    ) {
        if (options == null || !loaded) { Text(if (state is Load.Failed) "" else "Loading…"); return@FormScreen }
        FormImageField(
            "Cover image (optional, max 2 MB)",
            currentUrl = item?.imagePath?.takeIf { !removeCover },
            picked = cover,
            onPick = { cover = it },
            onRemoveCurrent = if (item?.imagePath != null) ({ removeCover = true }) else null,
        )
        FormTextField("Title", title, { title = it })
        FormTextField("Author", author, { author = it })
        ChoiceWithOther("Topic", options.topics, topic, { topic = it }, customTopic, { customTopic = it })
        FormDropdown("Level", options.levels.map { it to it }, level, { level = it })
        ChoiceWithOther("Language", options.languages, language, { language = it }, customLanguage, { customLanguage = it })
        FormTextField("PDF URL", pdfUrl, { pdfUrl = it }, keyboardType = KeyboardType.Uri, supporting = "Link to the PDF, starting with https://")
        FormSection("Visibility") {
            FormSwitch("Published", published, { published = it }, "Turn off to hide from the public library.")
            FeaturedSwitch(featured, { featured = it }, item?.featuredOnHomepage == true, options.featuredCount, options.featuredMax, "Featured Resources", "library", "resources")
        }
        if (item != null) Text("ID: ${item.id}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
