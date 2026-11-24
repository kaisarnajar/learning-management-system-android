package com.darsequran.academy.ui.manage.people

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.darsequran.academy.data.staff.formOf
import com.darsequran.academy.data.staff.people.ReviewsAdminApi
import com.darsequran.academy.data.staff.people.StudentReviewRow
import com.darsequran.academy.ui.common.Load
import com.darsequran.academy.ui.common.ReloadOnReturn
import com.darsequran.academy.ui.common.Tone
import com.darsequran.academy.ui.common.formatDate
import com.darsequran.academy.ui.manage.common.ConfirmDialog
import com.darsequran.academy.ui.manage.common.DetailCard
import com.darsequran.academy.ui.manage.common.Field
import com.darsequran.academy.ui.manage.common.FilterOption
import com.darsequran.academy.ui.manage.common.FormSwitch
import com.darsequran.academy.ui.manage.common.LoadedColumn
import com.darsequran.academy.ui.manage.common.ManageListScreen
import com.darsequran.academy.ui.manage.common.ManageScaffold
import com.darsequran.academy.ui.manage.common.Notice
import com.darsequran.academy.ui.manage.common.rememberActionRunner
import com.darsequran.academy.ui.manage.common.rememberLoader
import com.darsequran.academy.ui.manage.common.rememberPaged

private const val APPROVED = "approved"

fun reviewTone(status: String) = when (status) {
    "APPROVED" -> Tone.Success
    "REJECTED" -> Tone.Danger
    else -> Tone.Warning
}

private sealed interface ReviewAsk {
    val r: StudentReviewRow
    data class Approve(override val r: StudentReviewRow, val feature: Boolean = false) : ReviewAsk
    data class Reject(override val r: StudentReviewRow) : ReviewAsk
    data class Delete(override val r: StudentReviewRow) : ReviewAsk
}

/** Confirmations for review actions, shared by the list, detail and edit screens. */
@Composable
private fun ReviewAskDialog(ask: ReviewAsk?, runner: com.darsequran.academy.ui.manage.common.ActionRunner, onDismiss: () -> Unit, onDone: (ReviewAsk) -> Unit) {
    when (ask) {
        null -> Unit
        is ReviewAsk.Approve -> ConfirmDialog(
            "Approve review", if (ask.feature) "Approve this student review and show it on the homepage?" else "Approve this student review?", "Approve",
            onConfirm = {
                runner.run({ service<ReviewsAdminApi>().approve(ask.r.id, formOf { check("featureOnHomepage", ask.feature) }) }, success = "Review updated.") { onDone(ask) }
            },
            onDismiss = onDismiss,
        )
        is ReviewAsk.Reject -> ConfirmDialog(
            "Reject review", "Reject this review? The student can edit and resubmit.", "Reject", destructive = true,
            onConfirm = { runner.run({ service<ReviewsAdminApi>().reject(ask.r.id) }, success = "Review rejected. The student can edit and resubmit.") { onDone(ask) } },
            onDismiss = onDismiss,
        )
        is ReviewAsk.Delete -> ConfirmDialog(
            "Delete review by ${ask.r.user.name ?: ask.r.user.email}?", "The review is removed permanently.", "Delete", destructive = true,
            onConfirm = { runner.run({ service<ReviewsAdminApi>().delete(ask.r.id) }, success = "Review deleted.") { onDone(ask) } },
            onDismiss = onDismiss,
        )
    }
}

/** /admin/review-approvals: pending reviews to approve or reject, and every approved review (homepage visibility). */
@Composable
fun ReviewsListScreen(onBack: () -> Unit, onOpen: (String) -> Unit, onEdit: (String) -> Unit) {
    val vm = rememberPaged("admin-reviews") { page, q, type -> service<ReviewsAdminApi>().list(page, q.ifBlank { null }, type) }
    val summary = rememberLoader("admin-reviews-summary") { service<ReviewsAdminApi>().summary() }
    ReloadOnReturn { vm.refresh(); summary.reload() }
    val runner = rememberActionRunner()
    val listState by vm.state.collectAsStateWithLifecycle()
    val counts = (summary.state.collectAsStateWithLifecycle().value as? Load.Ready)?.value
    var asking by remember { mutableStateOf<ReviewAsk?>(null) }
    val approved = listState.filter == APPROVED

    ManageListScreen(
        title = "Reviews",
        onBack = onBack,
        vm = vm,
        key = { it.id },
        emptyTitle = if (approved) "No approved reviews yet" else "No student reviews awaiting approval",
        searchHint = "Name, e-mail, course or review text",
        filters = listOf(
            FilterOption(null, "Pending" + (counts?.pendingCount?.takeIf { it > 0 }?.let { " ($it)" } ?: "")),
            FilterOption(APPROVED, "Approved" + (counts?.approvedCount?.takeIf { it > 0 }?.let { " ($it)" } ?: "")),
        ),
        snackbar = runner.snackbar,
        header = {
            item {
                Text(
                    if (approved) "Every approved review stays here. Use Edit to add or remove homepage visibility." +
                        (counts?.let { " ${it.featuredCount} / ${it.featuredMax} featured on homepage." } ?: "")
                    else "Student reviews awaiting your approval before they can be featured.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    ) { r ->
        ActionCard(
            title = r.user.name ?: "—",
            subtitle = listOfNotNull(r.user.email, r.courseLabel, r.location?.ifBlank { null }).joinToString(" · "),
            meta = "Submitted " + formatDate(r.createdAt) + if (approved) (if (r.featuredOnHomepage) " · Featured" else " · Not featured") else "",
            body = "“${r.quote}”",
            leading = { Stars(r.rating, 14) },
            onClick = { onOpen(r.id) },
        ) {
            SmallOutlined("Edit", { onEdit(r.id) })
            if (approved) SmallDanger("Delete", { asking = ReviewAsk.Delete(r) }, enabled = !runner.busy)
            else {
                SmallPrimary("Approve", { asking = ReviewAsk.Approve(r) }, enabled = !runner.busy)
                SmallDanger("Reject", { asking = ReviewAsk.Reject(r) }, enabled = !runner.busy)
            }
        }
    }
    ReviewAskDialog(asking, runner, { asking = null }) { vm.refresh(); summary.reload() }
}

/** /admin/review-approvals/[id]. */
@Composable
fun ReviewDetailScreen(id: String, onBack: () -> Unit, onEdit: () -> Unit) {
    val loader = rememberLoader("admin-review-$id") { service<ReviewsAdminApi>().get(id) }
    ReloadOnReturn { loader.reload() }
    val runner = rememberActionRunner()
    val state by loader.state.collectAsStateWithLifecycle()
    var asking by remember { mutableStateOf<ReviewAsk?>(null) }
    val review = (state as? Load.Ready)?.value?.review

    ManageScaffold("Student review", onBack, runner.snackbar, actions = {
        if (review != null && review.status != "REJECTED") IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, "Edit") }
        if (review != null && review.status != "PENDING") IconButton(onClick = { asking = ReviewAsk.Delete(review) }) { Icon(Icons.Rounded.Delete, "Delete review") }
    }) { padding ->
        LoadedColumn(loader, padding) { data ->
            val r = data.review
            item {
                DetailCard {
                    Field("Name", r.user.name)
                    Field("Email", r.user.email)
                    Field("Course", r.courseLabel)
                    Field("Location", r.location)
                    com.darsequran.academy.ui.common.StatusChip(r.statusLabel.ifBlank { r.status }, reviewTone(r.status))
                    if (r.status == "APPROVED") Field("Homepage", if (r.featuredOnHomepage) "Featured" else "Not featured")
                }
            }
            item {
                DetailCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Stars(r.rating)
                        Text(formatDate(r.createdAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(r.quote, style = MaterialTheme.typography.bodyLarge)
                }
            }
            if (r.status == "PENDING") item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallPrimary("Approve", { asking = ReviewAsk.Approve(r) }, enabled = !runner.busy)
                    SmallDanger("Reject", { asking = ReviewAsk.Reject(r) }, enabled = !runner.busy)
                }
            }
        }
    }
    ReviewAskDialog(asking, runner, { asking = null }) { if (it is ReviewAsk.Delete) onBack() else loader.reload() }
}

/** /admin/review-approvals/[id]/edit: homepage visibility, and approve/reject while pending. */
@Composable
fun ReviewEditScreen(id: String, onBack: () -> Unit) {
    val loader = rememberLoader("admin-review-$id") { service<ReviewsAdminApi>().get(id) }
    val runner = rememberActionRunner()
    val state by loader.state.collectAsStateWithLifecycle()
    var asking by remember { mutableStateOf<ReviewAsk?>(null) }
    var feature by rememberSaveable { mutableStateOf(false) }
    var loaded by rememberSaveable { mutableStateOf(false) }
    val data = (state as? Load.Ready)?.value
    LaunchedEffect(data) { if (data != null && !loaded) { feature = data.review.featuredOnHomepage; loaded = true } }

    val title = when (data?.review?.status) { "PENDING" -> "Review submission"; else -> "Edit review settings" }
    ManageScaffold(title, onBack, runner.snackbar) { padding ->
        LoadedColumn(loader, padding) { d ->
            val r = d.review
            if (r.status == "REJECTED") {
                item { Notice("This review was rejected; the student can edit and resubmit it.", Tone.Info) }
                return@LoadedColumn
            }
            item { Notice("Review content was submitted by the student and cannot be changed here. You can manage homepage visibility below.", Tone.Warning) }
            item {
                DetailCard {
                    Field("Name", r.user.name)
                    Field("Email", r.user.email)
                    Field("Course", r.courseLabel)
                    Field("Location", r.location)
                    Stars(r.rating)
                    Field("Review", "“${r.quote}”")
                    Field("Submitted", formatDate(r.createdAt))
                }
            }
            item {
                DetailCard {
                    val canChoose = d.canFeature || feature
                    FormSwitch(
                        "Show on homepage", feature, { if (canChoose && !runner.busy) feature = it },
                        description = "Featured in the student reviews section on the homepage (up to ${d.featuredMax}; ${d.featuredCount}/${d.featuredMax} slots used).",
                    )
                    if (!d.canFeature) Text(
                        "The homepage already has ${d.featuredMax} reviews. Remove one before featuring this review.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (r.status == "PENDING") {
                        SmallPrimary("Approve", { asking = ReviewAsk.Approve(r, feature) }, enabled = !runner.busy)
                        SmallDanger("Reject", { asking = ReviewAsk.Reject(r) }, enabled = !runner.busy)
                    } else {
                        SmallPrimary("Save changes", {
                            runner.run({ service<ReviewsAdminApi>().saveHomepage(id, formOf { check("featuredOnHomepage", feature) }) }, success = "Homepage settings saved.") { onBack() }
                        }, enabled = !runner.busy)
                    }
                }
            }
        }
    }
    ReviewAskDialog(asking, runner, { asking = null }) { onBack() }
}
