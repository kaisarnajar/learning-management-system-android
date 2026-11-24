package com.darsequran.academy.ui.manage.bookstore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.darsequran.academy.data.staff.bookstore.BookOrderCounts
import com.darsequran.academy.data.staff.bookstore.BookOrderRow
import com.darsequran.academy.data.staff.bookstore.BookstoreAdminApi
import com.darsequran.academy.data.staff.formOf
import com.darsequran.academy.ui.common.AppCard
import com.darsequran.academy.ui.common.ReloadOnReturn
import com.darsequran.academy.ui.common.StatusChip
import com.darsequran.academy.ui.common.Tone
import com.darsequran.academy.ui.common.formatDate
import com.darsequran.academy.ui.common.formatDateTime
import com.darsequran.academy.ui.common.rupees
import com.darsequran.academy.ui.manage.common.ActionRunner
import com.darsequran.academy.ui.manage.common.ConfirmDialog
import com.darsequran.academy.ui.manage.common.DetailCard
import com.darsequran.academy.ui.manage.common.Divider
import com.darsequran.academy.ui.manage.common.Field
import com.darsequran.academy.ui.manage.common.FieldRow
import com.darsequran.academy.ui.manage.common.FilterOption
import com.darsequran.academy.ui.manage.common.LoadedColumn
import com.darsequran.academy.ui.manage.common.ManageListScreen
import com.darsequran.academy.ui.manage.common.ManageScaffold
import com.darsequran.academy.ui.manage.common.TextInputDialog
import com.darsequran.academy.ui.manage.common.rememberActionRunner
import com.darsequran.academy.ui.manage.common.rememberLoader
import com.darsequran.academy.ui.manage.common.rememberPaged
import com.darsequran.academy.ui.manage.money.ScreenshotThumb

/*
 * Book orders: /admin/bookstore/orders (pending, approved and completed tabs) and /admin/bookstore/orders/[id].
 * The website acts on orders from the list rows; the app offers the same actions on each card and on the detail.
 */

// The order endpoints don't report whether the e-mail went out, so the message doesn't claim it did.
private const val NOTIFIED = "The student will be notified."

/** The tab counts from the latest page of the book-order list, kept across rotation. */
class BookOrderBadges : ViewModel() {
    var counts by mutableStateOf<BookOrderCounts?>(null)
}

@Composable
fun BookOrdersListScreen(onBack: () -> Unit, onOpen: (String) -> Unit, onBooks: () -> Unit) {
    // The tab badges (every tab's total for the current search) come with each page of the list; they used to be
    // fetched again with a second list request.
    val badges: BookOrderBadges = viewModel()
    val vm = rememberPaged("admin-book-orders") { page, q, status ->
        service<BookstoreAdminApi>().orders(page, q.ifBlank { null }, status).also { badges.counts = it.counts }.paged()
    }
    val listState by vm.state.collectAsStateWithLifecycle()
    val c = badges.counts
    val runner = rememberActionRunner()
    val refresh: () -> Unit = { vm.refresh() }
    ReloadOnReturn(refresh)

    fun label(name: String, n: Int?) = if (n != null && n > 0) "$name ($n)" else name
    ManageListScreen(
        title = "Book orders",
        onBack = onBack,
        vm = vm,
        key = { it.id },
        emptyTitle = when (listState.filter) {
            "approved" -> if (listState.query.isBlank()) "No approved orders awaiting fulfillment." else "No approved orders match your search."
            "completed" -> if (listState.query.isBlank()) "No completed orders found." else "No completed orders match your search."
            else -> if (listState.query.isBlank()) "No book orders awaiting verification." else "No pending orders match your search."
        },
        searchHint = "Name, e-mail or transaction ID",
        filters = listOf(
            FilterOption(null, label("Pending approvals", c?.pending)),
            FilterOption("approved", label("Approved", c?.approved)),
            FilterOption("completed", label("Completed", c?.completed)),
        ),
        snackbar = runner.snackbar,
        actions = { IconButton(onClick = onBooks) { Icon(Icons.AutoMirrored.Rounded.MenuBook, "Manage books") } },
    ) { order ->
        BookOrderCard(order, runner, onOpen = { onOpen(order.id) }, onChanged = refresh)
    }
}

@Composable
private fun BookOrderCard(order: BookOrderRow, runner: ActionRunner, onOpen: () -> Unit, onChanged: () -> Unit) {
    AppCard(Modifier.fillMaxWidth(), onClick = onOpen, contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(order.customerName ?: "—", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(order.customerEmail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Order ${order.shortId} · ${formatDate(order.createdAt)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(rupees(order.totalAmountInrPaise), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                StatusChip(order.statusLabel.ifBlank { order.status }, bookOrderTone(order.status))
            }
        }
        Spacer(Modifier.height(8.dp))
        order.items.forEach { item ->
            Text(
                "${item.title} × ${item.quantity} — ${rupees(item.lineTotalInrPaise)}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            order.deliveryAddress?.let { addr ->
                listOfNotNull(addr, order.deliveryPinCode?.let { "PIN: $it" }, order.deliveryPhoneNumber?.let { "Phone: $it" }).joinToString("\n")
            } ?: "No address provided",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            listOfNotNull(paymentMethodLabel(order.paymentMethod), order.upiTransactionId?.let { "Ref. $it" }).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
        )
        if (order.status == "SHIPPED" && order.courierServiceName != null) {
            Text(
                "Courier: ${order.courierServiceName} · Tracking ID: ${order.trackingId ?: "—"}",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            order.paymentScreenshotPath?.let { PaymentProof(it) }
            BookOrderActions(order.id, order.status, runner, onChanged)
        }
    }
}

fun paymentMethodLabel(method: String?): String = when (method?.lowercase()) {
    null, "" -> "—"
    "upi" -> "UPI"
    "bank", "bank_transfer" -> "Bank transfer"
    "cash" -> "Cash"
    else -> method.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

/**
 * The order's payment proof: a screenshot, or a PDF (which an image view showed as broken). The website tells them
 * apart by the file name, so this does too.
 */
@Composable
private fun PaymentProof(path: String) = ScreenshotThumb(path, isPdf = isPdfProof(path))

internal fun isPdfProof(path: String) = path.substringBefore('?').endsWith(".pdf", ignoreCase = true)

/** The status changes the website offers for an order in [status]. */
@Composable
fun BookOrderActions(id: String, status: String, runner: ActionRunner, onChanged: () -> Unit) {
    var confirm by remember { mutableStateOf<String?>(null) }
    var shipping by remember { mutableStateOf(false) }
    var voiding by remember { mutableStateOf(false) }

    fun act(action: String, success: String, fields: com.darsequran.academy.data.staff.FormFields.() -> Unit = {}) =
        runner.run({ service<BookstoreAdminApi>().orderAction(id, action, formOf(fields)) }, success = success) { onChanged() }

    when (status) {
        "PENDING_VERIFICATION" -> {
            Button(onClick = { confirm = "approve" }, enabled = !runner.busy) { Text("Approve") }
            OutlinedButton(onClick = { confirm = "decline" }, enabled = !runner.busy) { Text("Decline", color = MaterialTheme.colorScheme.error) }
        }
        "APPROVED" -> {
            Button(onClick = { shipping = true }, enabled = !runner.busy) { Text("Mark as shipped") }
            OutlinedButton(onClick = { confirm = "refund" }, enabled = !runner.busy) { Text("Refund", color = MaterialTheme.colorScheme.error) }
        }
        "SHIPPED" -> OutlinedButton(onClick = { confirm = "refund" }, enabled = !runner.busy) { Text("Refund", color = MaterialTheme.colorScheme.error) }
        "DECLINED", "REFUNDED" -> OutlinedButton(onClick = { voiding = true }, enabled = !runner.busy) { Text("Void", color = MaterialTheme.colorScheme.error) }
    }

    when (confirm) {
        "approve" -> ConfirmDialog(
            "Approve order", "Approve this book order and notify the student?", "Approve",
            onConfirm = { act("approve", "Order approved. $NOTIFIED") }, onDismiss = { confirm = null },
        )
        "decline" -> ConfirmDialog(
            "Decline order", "Are you sure you want to decline this order?", "Decline", destructive = true,
            onConfirm = { act("decline", "Order declined. $NOTIFIED") }, onDismiss = { confirm = null },
        )
        "refund" -> ConfirmDialog(
            "Refund order",
            if (status == "SHIPPED") "Refund this shipped order? The recorded payment will be voided and the student notified."
            else "Are you sure you want to refund this order? The student will be notified.",
            "Refund", destructive = true,
            onConfirm = { act("refund", "Order refunded. $NOTIFIED") }, onDismiss = { confirm = null },
        )
    }
    if (voiding) TextInputDialog(
        title = "Void order",
        label = "Reason for voiding",
        message = "Void this order? It stays on record for auditing but disappears from order lists and finance.",
        confirmLabel = "Void", required = true, destructive = true,
        onConfirm = { reason -> act("void", "Order voided.") { text("reason", reason) } },
        onDismiss = { voiding = false },
    )
    if (shipping) ShipDialog(
        onConfirm = { courier, tracking ->
            act("ship", "Order marked as shipped. $NOTIFIED") { text("courierServiceName", courier); text("trackingId", tracking) }
        },
        onDismiss = { shipping = false },
    )
}

@Composable
private fun ShipDialog(onConfirm: (String, String) -> Unit, onDismiss: () -> Unit) {
    var courier by remember { mutableStateOf("") }
    var tracking by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mark as shipped") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Enter shipment details to notify the student.", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    courier, { courier = it.take(100) }, label = { Text("Courier service name") }, singleLine = true,
                    placeholder = { Text("e.g. DTDC, India Post") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    tracking, { tracking = it.take(100) }, label = { Text("Tracking ID") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onDismiss(); onConfirm(courier.trim(), tracking.trim()) }, enabled = courier.isNotBlank() && tracking.isNotBlank()) {
                Text("Confirm shipment")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun BookOrderDetailScreen(id: String, onBack: () -> Unit) {
    val loader = rememberLoader("admin-book-order-$id") { service<BookstoreAdminApi>().order(id) }
    ReloadOnReturn { loader.reload() }
    val runner = rememberActionRunner()

    ManageScaffold("Book order", onBack, runner.snackbar) { padding ->
        LoadedColumn(loader, padding) { r ->
            val o = r.order
            item {
                DetailCard {
                    Text("Order ${o.shortId}", style = MaterialTheme.typography.titleLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusChip(o.statusLabel.ifBlank { o.status }, bookOrderTone(o.status))
                        if (o.deleted) StatusChip("Deleted", Tone.Danger)
                    }
                    Text("Placed ${formatDateTime(o.createdAt)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!o.deleted) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            BookOrderActions(o.id, o.status, runner) { loader.reload() }
                        }
                    }
                }
            }
            item {
                DetailCard("Customer & delivery") {
                    Field("Name", o.customer.name)
                    Field("Email", o.customer.email)
                    Field("Phone", o.deliveryPhoneNumber)
                    Field("Address", listOfNotNull(o.deliveryAddress, o.deliveryPinCode).joinToString(" — "))
                    Field("Notes", o.notes, showEmpty = false)
                }
            }
            item {
                DetailCard("Payment & shipment") {
                    Field("Method", paymentMethodLabel(o.paymentMethod))
                    Field("UTR / reference", o.upiTransactionId)
                    Column {
                        Text("Proof", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (o.paymentScreenshotPath != null) PaymentProof(o.paymentScreenshotPath)
                        else Text("No screenshot uploaded", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (o.courierServiceName != null) {
                        Field("Courier", o.courierServiceName)
                        Field("Tracking ID", o.trackingId)
                    }
                }
            }
            item {
                DetailCard("Items") {
                    o.items.forEach { item ->
                        Row(verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f)) {
                                Text(item.title, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${item.author} · ${rupees(item.priceAtPurchaseInrPaise)} × ${item.quantity}",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(rupees(item.lineTotalInrPaise), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Divider()
                    FieldRow("Subtotal", rupees(o.subtotalInrPaise))
                    FieldRow("Shipping", rupees(o.shippingChargeInrPaise))
                    FieldRow("Total", rupees(o.totalAmountInrPaise), strong = true)
                }
            }
            item {
                DetailCard("Timeline") {
                    TimelineEntry("Order placed", null, null, o.createdAt)
                    r.timeline.forEach { e -> TimelineEntry(e.label.ifBlank { e.action }, e.actorEmail, e.reason, e.createdAt) }
                    if (r.timeline.isEmpty()) Text(
                        "No admin actions recorded for this order yet. Status changes made before the audit log was introduced are not listed.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineEntry(title: String, actor: String?, reason: String?, at: String?) {
    Column(Modifier.padding(start = 4.dp)) {
        Text(title + (actor?.let { " by $it" } ?: ""), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        if (!reason.isNullOrBlank()) Text("Reason: $reason", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(formatDateTime(at), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
