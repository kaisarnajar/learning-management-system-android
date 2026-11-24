package com.darsequran.academy.ui.manage.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Button
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.darsequran.academy.data.staff.PickedFile
import com.darsequran.academy.data.staff.formOf
import com.darsequran.academy.ui.common.Banner
import com.darsequran.academy.ui.common.HtmlText
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
import com.darsequran.academy.ui.manage.common.FormScreen
import com.darsequran.academy.ui.manage.common.FormSection
import com.darsequran.academy.ui.manage.common.FormSwitch
import com.darsequran.academy.ui.manage.common.FormTextField
import com.darsequran.academy.ui.manage.common.LoadedColumn
import com.darsequran.academy.ui.manage.common.ManageListScreen
import com.darsequran.academy.ui.manage.common.ManageScaffold
import com.darsequran.academy.ui.manage.common.RecordCard
import com.darsequran.academy.ui.manage.common.RichTextField
import com.darsequran.academy.ui.manage.common.RowIcon
import com.darsequran.academy.ui.manage.common.rememberActionRunner
import com.darsequran.academy.ui.manage.common.rememberLoader
import com.darsequran.academy.ui.manage.common.rememberPaged

// Website: /admin/blogs (+ new, [id]/edit), /admin/blog-approvals (+ [id]), /teacher/blogs (+ new, [id]/edit).

enum class BlogEditorMode { Admin, Teacher }

private fun BlogRow.metaLine(withAuthor: Boolean): String = listOfNotNull(
    authorName?.takeIf { withAuthor },
    formatDate(createdAt),
    imageCount.takeIf { it > 0 }?.let { if (it == 1) "1 image" else "$it images" },
    "Featured".takeIf { featuredOnHomepage },
).joinToString(" · ")

@Composable
private fun BlogLeading(row: BlogRow) { if (row.thumbnail != null) RowThumbnail(row.thumbnail) else RowIcon(Icons.AutoMirrored.Rounded.Article) }

// --------------------------------------------------------------- admin list

@Composable
fun BlogsListScreen(onBack: () -> Unit, onOpen: (String) -> Unit, onNew: () -> Unit) {
    val vm = rememberPaged("admin-blogs") { page, q, _ -> service<ContentApi>().blogs(page, q.ifBlank { null }) }
    ReloadOnReturn(vm::refresh)
    val runner = rememberActionRunner()
    val context = LocalContext.current
    var deleting by remember { mutableStateOf<BlogRow?>(null) }

    ManageListScreen(
        title = "Blogs",
        onBack = onBack,
        vm = vm,
        key = { it.id },
        emptyTitle = "No blog posts yet",
        searchHint = "Search by title or author",
        snackbar = runner.snackbar,
        fab = { AddFab("New blog post", onNew) },
        header = {
            item {
                Text(
                    "Write articles, or review teacher submissions in Blog approvals.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    ) { post ->
        RecordCard(
            title = post.title,
            subtitle = post.excerpt,
            meta = post.metaLine(withAuthor = true),
            chip = post.statusLabel to contentTone(post.statusLabel),
            leading = { BlogLeading(post) },
            trailing = {
                RowMenu(
                    listOfNotNull(
                        RowAction("Edit") { onOpen(post.id) },
                        post.publicPath?.let { path -> RowAction("View on website") { openWeb(context, path) } },
                        RowAction("Delete", destructive = true) { deleting = post },
                    ),
                )
            },
            onClick = { onOpen(post.id) },
        )
    }
    deleting?.let { post -> DeleteBlogDialog(post.title, { deleting = null }) { runner.run({ service<ContentApi>().deleteBlog(post.id) }, success = "Blog post deleted.") { vm.refresh() } } }
}

@Composable
private fun DeleteBlogDialog(title: String, onDismiss: () -> Unit, onConfirm: () -> Unit) =
    ConfirmDialog("Delete this blog post?", "\"$title\", its images, likes and comments are removed for good.", "Delete", onConfirm, onDismiss, destructive = true)

// ------------------------------------------------------------ teacher list

@Composable
fun TeacherBlogsListScreen(onBack: () -> Unit, onEdit: (String) -> Unit, onNew: () -> Unit) {
    val vm = rememberPaged("teacher-blogs") { page, _, _ -> service<ContentApi>().teacherBlogs(page) }
    ReloadOnReturn(vm::refresh)
    val runner = rememberActionRunner()
    val context = LocalContext.current
    var deleting by remember { mutableStateOf<BlogRow?>(null) }

    ManageListScreen(
        title = "My blog posts",
        onBack = onBack,
        vm = vm,
        key = { it.id },
        emptyTitle = "You have not written any blog posts yet",
        searchHint = null,
        snackbar = runner.snackbar,
        fab = { AddFab("New blog post", onNew) },
        header = {
            item {
                Text(
                    "Write articles for the academy blog. Posts are reviewed by an admin before they go live.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    ) { post ->
        RecordCard(
            title = post.title,
            subtitle = post.excerpt,
            meta = "Updated " + formatDate(post.updatedAt) + if (!post.canEdit) " · Approved posts can no longer be edited" else "",
            chip = post.statusLabel to contentTone(post.statusLabel),
            leading = { BlogLeading(post) },
            trailing = {
                RowMenu(
                    listOfNotNull(
                        if (post.canEdit) RowAction("Edit") { onEdit(post.id) } else null,
                        post.publicPath?.let { path -> RowAction("View on website") { openWeb(context, path) } },
                        if (post.canDelete) RowAction("Delete", destructive = true) { deleting = post } else null,
                    ),
                )
            },
            onClick = when {
                post.canEdit -> { { onEdit(post.id) } }
                post.publicPath != null -> { { openWeb(context, post.publicPath) } }
                else -> null
            },
        )
    }
    deleting?.let { post ->
        DeleteBlogDialog(post.title, { deleting = null }) { runner.run({ service<ContentApi>().deleteTeacherBlog(post.id) }, success = "Blog post deleted.") { vm.refresh() } }
    }
}

// ------------------------------------------------------------------ editor

/**
 * New or edit post, for admins ([BlogEditorMode.Admin]: publish and homepage switches; a teacher's submission is
 * read-only apart from those) and teachers (always sent for approval; approved posts are locked).
 */
@Composable
fun BlogEditorScreen(mode: BlogEditorMode, id: String?, onBack: () -> Unit) {
    val admin = mode == BlogEditorMode.Admin
    val loader = rememberLoader("blog-editor-$mode-${id ?: "new"}") {
        val api = service<ContentApi>()
        when {
            id != null && admin -> api.blog(id)
            id != null -> api.teacherBlog(id)
            else -> (if (admin) api.blogLimits() else api.teacherBlogLimits())
                .let { BlogResponse(BlogDetail(id = ""), it.featuredCount, it.featuredMax, it.maxImages) }
        }
    }
    val runner = rememberActionRunner()
    val state by loader.state.collectAsStateWithLifecycle()
    val data = (state as? Load.Ready)?.value
    val post = data?.post

    var loaded by rememberSaveable { mutableStateOf(id == null) }
    var title by rememberSaveable { mutableStateOf("") }
    var excerpt by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    var published by rememberSaveable { mutableStateOf(false) }
    var featured by rememberSaveable { mutableStateOf(false) }
    var removed by remember { mutableStateOf(emptySet<String>()) }
    var picked by remember { mutableStateOf(emptyList<PickedFile>()) }
    var confirmDelete by remember { mutableStateOf(false) }
    LaunchedEffect(post) {
        if (post != null && !loaded) {
            title = post.title; excerpt = post.excerpt.orEmpty(); body = post.body
            published = post.published; featured = post.featuredOnHomepage; loaded = true
        }
    }
    val readOnly = admin && post?.contentReadOnly == true
    val locked = !admin && id != null && post?.canEdit == false

    val saveLabel = when {
        !admin && id == null -> "Submit for approval"
        !admin -> "Resubmit for approval"
        id == null -> "Create blog post"
        else -> "Save changes"
    }
    FormScreen(
        title = when {
            readOnly -> "Review blog post"
            id == null -> "New blog post"
            else -> "Edit blog post"
        },
        onBack = onBack,
        saving = runner.busy,
        snackbar = runner.snackbar,
        saveLabel = saveLabel,
        error = (state as? Load.Failed)?.message,
        onSave = {
            if (locked) { runner.message("This post can no longer be edited because it has been approved."); return@FormScreen }
            // A teacher's submission keeps its text: send it back unchanged, as the website's read-only inputs do.
            val bodyHtml = if (readOnly) post.body else toWebsiteHtml(body)
            if (!readOnly && isBlankHtml(bodyHtml)) { runner.message("Write the blog content first."); return@FormScreen }
            val parts = formOf {
                text("title", title.trim()); text("excerpt", excerpt.trim()); text("body", bodyHtml)
                if (admin) { check("published", published); check("featuredOnHomepage", featured) }
                if (!readOnly) { texts("removeImage", removed); picked.forEach { file("images", it) } }
            }
            val api: suspend com.darsequran.academy.AppContainer.() -> com.darsequran.academy.data.staff.ActionResponse = {
                val s = service<ContentApi>()
                when {
                    admin && id == null -> s.createBlog(parts)
                    admin -> s.updateBlog(id!!, parts)
                    id == null -> s.createTeacherBlog(parts)
                    else -> s.updateTeacherBlog(id, parts)
                }
            }
            runner.run(
                api,
                success = when {
                    !admin && id == null -> "Your blog post was submitted for admin approval."
                    !admin -> "Your changes were resubmitted for admin approval."
                    id == null -> "Blog post created."
                    else -> "Changes saved."
                },
            ) { onBack() }
        },
    ) {
        if (data == null || !loaded) { Text(if (state is Load.Failed) "" else "Loading…"); return@FormScreen }
        if (id == null) Text(
            if (admin) "Write your article and attach screenshots or photos. Use \"Show on homepage\" to feature up to ${data.featuredMax} published posts; all published posts appear on the Blog page."
            else "Share guidance or reflections with students. Your post will be reviewed before it appears on the public blog.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (post != null && id != null) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip(post.statusLabel, contentTone(post.statusLabel))
            post.authorName?.let { Text("by $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        if (readOnly) Banner(
            "This post was submitted by a teacher for approval. Title, summary, content and images cannot be changed here. You can update publishing and homepage settings below, or use Approve / Reject on Blog approvals.",
            Tone.Warning,
        )
        if (locked) Banner("This post can no longer be edited because it has been approved.", Tone.Warning)

        FormTextField("Title", title, { title = it }, maxLength = 200, enabled = !readOnly && !locked, supporting = "At least 3 characters, e.g. Tips for memorizing Quran with tajweed")
        FormTextField("Short summary (optional)", excerpt, { excerpt = it }, singleLine = false, minLines = 2, maxLength = 400, enabled = !readOnly && !locked, supporting = "A brief line shown on the blog listing page.")
        if (readOnly || locked) {
            DetailCard("Blog content") { HtmlText(body) }
        } else {
            RichTextField("Blog content", body, { body = it })
            Text("At least 20 characters. You can add screenshots below.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ImagesField(
            label = if (id == null) "Screenshots & photos (optional)" else "Screenshots & photos",
            current = post?.images.orEmpty(), removed = removed, onRemovedChange = { removed = it },
            picked = picked, onPickedChange = { picked = it }, max = data.maxImages, readOnly = readOnly || locked,
        )
        if (admin) {
            val canFeature = post?.featuredOnHomepage == true || data.featuredCount < data.featuredMax
            FormSection("Publishing") {
                FormSwitch("Publish on the public blog", published, { published = it }, "Leave off to save as a draft.")
                FormSwitch(
                    "Show on homepage", featured && canFeature, { if (canFeature) featured = it },
                    featuredDescription(canFeature, data.featuredCount, data.featuredMax, "Featured Blogs", "Blog page", "posts"),
                )
            }
        } else if (!locked) {
            Banner("Your post will be sent to the admin for review. Once approved, it will appear on the public blog page.", Tone.Warning)
        }
        if (id != null && (admin || post?.canDelete == true)) {
            TextButton(onClick = { confirmDelete = true }) { Text("Delete this post", color = MaterialTheme.colorScheme.error) }
        }
    }

    if (confirmDelete && id != null) DeleteBlogDialog(title, { confirmDelete = false }) {
        runner.run({ if (admin) service<ContentApi>().deleteBlog(id) else service<ContentApi>().deleteTeacherBlog(id) }, success = "Blog post deleted.") { onBack() }
    }
}

// -------------------------------------------------------------- approvals

@Composable
fun BlogApprovalsListScreen(onBack: () -> Unit, onOpen: (String) -> Unit, onEdit: (String) -> Unit) {
    val vm = rememberPaged("admin-blog-approvals") { page, q, _ -> service<ContentApi>().blogApprovals(page, q.ifBlank { null }) }
    ReloadOnReturn(vm::refresh)
    val runner = rememberActionRunner()
    var deciding by remember { mutableStateOf<Pair<BlogRow, Boolean>?>(null) }

    ManageListScreen(
        title = "Blog approvals",
        onBack = onBack,
        vm = vm,
        key = { it.id },
        emptyTitle = "No teacher blog posts awaiting approval",
        searchHint = "Search by title or author",
        snackbar = runner.snackbar,
        header = {
            item {
                Text(
                    "Review blog posts submitted by teachers before they appear on the public blog. Admin posts are published directly from Blogs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    ) { post ->
        RecordCard(
            title = post.title,
            subtitle = listOfNotNull(post.authorName, post.authorEmail).joinToString(" · ").ifBlank { null },
            meta = "Submitted " + formatDate(post.createdAt) + " · " + if (post.imageCount == 1) "1 image" else "${post.imageCount} images",
            chip = post.statusLabel to contentTone(post.statusLabel),
            leading = { BlogLeading(post) },
            trailing = {
                RowMenu(
                    listOf(
                        RowAction("View") { onOpen(post.id) },
                        RowAction("Edit") { onEdit(post.id) },
                        RowAction("Approve") { deciding = post to true },
                        RowAction("Reject", destructive = true) { deciding = post to false },
                    ),
                )
            },
            onClick = { onOpen(post.id) },
        )
    }
    deciding?.let { (post, approve) ->
        ApprovalDialog(approve, onDismiss = { deciding = null }) {
            runner.run(
                { if (approve) service<ContentApi>().approveBlog(post.id) else service<ContentApi>().rejectBlog(post.id) },
                success = if (approve) "Blog post approved and published." else "Blog post rejected. The teacher can edit and resubmit.",
            ) { vm.refresh() }
        }
    }
}

@Composable
private fun ApprovalDialog(approve: Boolean, onDismiss: () -> Unit, onConfirm: () -> Unit) = ConfirmDialog(
    if (approve) "Approve blog post" else "Reject blog post",
    if (approve) "Approve this blog post and publish it on the public blog?" else "Reject this blog post and mark it as declined? The teacher can edit and resubmit it.",
    if (approve) "Approve" else "Reject",
    onConfirm, onDismiss, destructive = !approve,
)

@Composable
fun BlogApprovalDetailScreen(id: String, onBack: () -> Unit, onEdit: () -> Unit) {
    val loader = rememberLoader("admin-blog-approval-$id") { service<ContentApi>().blogApproval(id).post }
    ReloadOnReturn { loader.reload() }
    val runner = rememberActionRunner()
    var deciding by remember { mutableStateOf<Boolean?>(null) }

    ManageScaffold("Blog approval", onBack, runner.snackbar, actions = {
        IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, "Edit") }
    }) { padding ->
        LoadedColumn(loader, padding) { post ->
            item {
                DetailCard {
                    Text(post.title, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Submitted by " + (post.authorName ?: post.authorEmail ?: "Teacher") + if (post.authorName != null && post.authorEmail != null) " · ${post.authorEmail}" else "",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { deciding = true }, enabled = !runner.busy) { Icon(Icons.Rounded.Check, null); Text(" Approve") }
                        OutlinedButton(onClick = { deciding = false }, enabled = !runner.busy) {
                            Icon(Icons.Rounded.Close, null, tint = MaterialTheme.colorScheme.error); Text(" Reject", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            item {
                DetailCard {
                    Field("Submitted", formatDate(post.createdAt))
                    Field("Images", post.images.size.toString())
                    Field("Excerpt", post.excerpt, showEmpty = false)
                }
            }
            item { DetailCard("Content") { HtmlText(post.body) } }
            if (post.images.isNotEmpty()) item {
                DetailCard("Images") {
                    post.images.forEach { img ->
                        RemoteImage(img.imagePath, img.caption, Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)))
                        img.caption?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
    }
    deciding?.let { approve ->
        ApprovalDialog(approve, onDismiss = { deciding = null }) {
            runner.run(
                { if (approve) service<ContentApi>().approveBlog(id) else service<ContentApi>().rejectBlog(id) },
                success = if (approve) "Blog post approved and published." else "Blog post rejected.",
            ) { onBack() }
        }
    }
}
