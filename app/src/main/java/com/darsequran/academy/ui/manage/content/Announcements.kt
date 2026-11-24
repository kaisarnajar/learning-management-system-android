package com.darsequran.academy.ui.manage.content

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.darsequran.academy.data.staff.formOf
import com.darsequran.academy.ui.common.Load
import com.darsequran.academy.ui.common.ReloadOnReturn
import com.darsequran.academy.ui.common.formatDate
import com.darsequran.academy.ui.common.openWeb
import com.darsequran.academy.ui.manage.common.AddFab
import com.darsequran.academy.ui.manage.common.ConfirmDialog
import com.darsequran.academy.ui.manage.common.FormDateField
import com.darsequran.academy.ui.manage.common.FormScreen
import com.darsequran.academy.ui.manage.common.FormSection
import com.darsequran.academy.ui.manage.common.FormSwitch
import com.darsequran.academy.ui.manage.common.FormTextField
import com.darsequran.academy.ui.manage.common.ManageListScreen
import com.darsequran.academy.ui.manage.common.RecordCard
import com.darsequran.academy.ui.manage.common.RowIcon
import com.darsequran.academy.ui.manage.common.rememberActionRunner
import com.darsequran.academy.ui.manage.common.rememberLoader
import com.darsequran.academy.ui.manage.common.rememberPaged
import com.darsequran.academy.ui.manage.common.rememberFormFiles
import com.darsequran.academy.ui.manage.common.FeaturedSwitch

// Website: /admin/announcements (list, homepage toggle, delete), /new and /[id]/edit.

@Composable
fun AnnouncementsListScreen(onBack: () -> Unit, onOpen: (String) -> Unit, onNew: () -> Unit) {
    val vm = rememberPaged("admin-announcements") { page, q, _ -> service<ContentApi>().announcements(page, q.ifBlank { null }) }
    ReloadOnReturn(vm::refresh)
    val runner = rememberActionRunner()
    val context = LocalContext.current
    var deleting by remember { mutableStateOf<AnnouncementRow?>(null) }

    ManageListScreen(
        title = "Announcements",
        onBack = onBack,
        vm = vm,
        key = { it.id },
        emptyTitle = "No announcements yet",
        searchHint = "Search by title, location or text",
        snackbar = runner.snackbar,
        fab = { AddFab("New announcement", onNew) },
        header = {
            item {
                Text(
                    "Academy-wide news: events, scholar visits, masjid programs. Up to 4 published items can be featured on the homepage; every published item appears on the public Announcements page.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    ) { a ->
        RecordCard(
            title = a.title,
            subtitle = listOfNotNull(a.eventDate, a.location).joinToString(" · ").ifBlank { null },
            meta = listOfNotNull(
                "Created " + formatDate(a.createdAt),
                a.imageCount.takeIf { it > 0 }?.let { if (it == 1) "1 image" else "$it images" },
                "Publish first to feature".takeIf { !a.published },
            ).joinToString(" · "),
            chip = a.statusLabel to contentTone(a.statusLabel),
            leading = { if (a.thumbnail != null) RowThumbnail(a.thumbnail) else RowIcon(Icons.Rounded.Campaign) },
            trailing = {
                RowMenu(
                    listOfNotNull(
                        RowAction("Edit") { onOpen(a.id) },
                        if (a.published) RowAction(if (a.showOnHomepage) "Remove from homepage" else "Add to homepage") {
                            runner.run(
                                { service<ContentApi>().toggleAnnouncementHomepage(a.id) },
                                success = if (a.showOnHomepage) "Removed from the homepage." else "Featured on the homepage.",
                            ) { vm.refresh() }
                        } else null,
                        if (a.published) RowAction("View on website") { openWeb(context, "/announcements/${a.id}") } else null,
                        RowAction("Delete", destructive = true) { deleting = a },
                    ),
                )
            },
            onClick = { onOpen(a.id) },
        )
    }

    deleting?.let { a ->
        ConfirmDialog(
            "Delete this announcement?", "\"${a.title}\" and its images are removed for good.", "Delete", destructive = true,
            onConfirm = { runner.run({ service<ContentApi>().deleteAnnouncement(a.id) }, success = "Announcement deleted.") { vm.refresh() } },
            onDismiss = { deleting = null },
        )
    }
}

/** New ([id] null) or edit announcement. */
@Composable
fun AnnouncementFormScreen(id: String?, onBack: () -> Unit) {
    val loader = rememberLoader("admin-announcement-form-${id ?: "new"}") {
        val api = service<ContentApi>()
        if (id != null) api.announcement(id)
        else api.announcementLimits().let { AnnouncementResponse(AnnouncementDetail(id = ""), it.featuredCount, it.featuredMax, it.maxImages) }
    }
    val runner = rememberActionRunner()
    val state by loader.state.collectAsStateWithLifecycle()
    val data = (state as? Load.Ready)?.value

    var loaded by rememberSaveable { mutableStateOf(false) }
    var title by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    var eventDate by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var published by rememberSaveable { mutableStateOf(true) }
    var homepage by rememberSaveable { mutableStateOf(false) }
    val files = rememberFormFiles("announcement-files-${id ?: "new"}")
    var removed by files::removed
    var picked by files::picked
    LaunchedEffect(data) {
        val a = data?.announcement ?: return@LaunchedEffect
        if (!loaded && id != null) {
            title = a.title; body = a.body; eventDate = a.eventDateInput.orEmpty(); location = a.location.orEmpty()
            published = a.published; homepage = a.showOnHomepage
        }
        loaded = true
    }

    FormScreen(
        title = if (id == null) "New announcement" else "Edit announcement",
        onBack = onBack,
        saving = runner.busy,
        snackbar = runner.snackbar,
        saveLabel = if (id == null) "Publish announcement" else "Save changes",
        canSave = loaded,
        loadError = (state as? Load.Failed)?.message,
        onRetry = { loader.reload(true) },
        onSave = {
            val parts = formOf {
                text("title", title.trim()); text("body", body); text("eventDate", eventDate); text("location", location.trim())
                check("published", published); check("showOnHomepage", homepage)
                texts("removeImage", removed)
                picked.forEach { file("images", it) }
            }
            runner.run(
                { if (id == null) service<ContentApi>().createAnnouncement(parts) else service<ContentApi>().updateAnnouncement(id, parts) },
                success = if (id == null) "Announcement created." else "Announcement updated.",
            ) { onBack() }
        },
    ) {
        if (data == null) { Text(if (state is Load.Failed) "" else "Loading…"); return@FormScreen }
        if (id == null) Text(
            "Share events such as guest scholars at a masjid. Use \"Show on homepage\" to feature up to ${data.featuredMax} items; every published item appears on the Announcements page.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FormTextField("Title", title, { title = it }, maxLength = 200, supporting = "e.g. Guest Mufti visit — Masjid Al-Noor")
        FormDateField("Event date (optional)", eventDate, { eventDate = it }, clearable = true)
        FormTextField("Location (optional)", location, { location = it }, maxLength = 200, supporting = "e.g. Masjid Al-Noor, Srinagar")
        FormTextField("Announcement", body, { body = it }, singleLine = false, minLines = 6, supporting = "Full details for students and visitors (at least 10 characters).")
        ImagesField(
            label = if (id == null) "Screenshots & photos (optional)" else "Screenshots & photos",
            current = data.announcement.images, removed = removed, onRemovedChange = { removed = it },
            picked = picked, onPickedChange = { picked = it }, max = data.maxImages,
        )
        FormSection("Visibility") {
            FeaturedSwitch(homepage, { homepage = it }, data.announcement.showOnHomepage, data.featuredCount, data.featuredMax, "Featured Announcements", "Announcements page", "announcements")
            FormSwitch("Published", published, { published = it; if (!it) homepage = false }, "Turn off to save as a draft (hidden from the public site). Publishing notifies every student.")
        }
    }
}
