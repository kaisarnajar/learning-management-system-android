package com.darsequran.academy.ui.manage.money

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.darsequran.academy.data.remote.Paged
import com.darsequran.academy.data.staff.money.MoneyApi
import com.darsequran.academy.data.staff.money.MoneyRow
import com.darsequran.academy.ui.common.AppCard
import com.darsequran.academy.ui.common.PagedListBody
import com.darsequran.academy.ui.common.ReloadOnReturn
import com.darsequran.academy.ui.common.StatusChip
import com.darsequran.academy.ui.common.Tone
import com.darsequran.academy.ui.common.formatDate
import com.darsequran.academy.ui.manage.bookstore.BookstoreAdminApi
import com.darsequran.academy.ui.manage.common.ActionRunner
import com.darsequran.academy.ui.manage.common.ConfirmDialog
import com.darsequran.academy.ui.manage.common.FilterChips
import com.darsequran.academy.ui.manage.common.FilterOption
import com.darsequran.academy.ui.manage.common.ManageScaffold
import com.darsequran.academy.ui.manage.common.TextInputDialog
import com.darsequran.academy.ui.manage.common.rememberActionRunner
import com.darsequran.academy.ui.manage.common.rememberPaged
import com.darsequran.academy.ui.theme.Brand

// ---------------------------------------------------------- /admin/payments

/** Pending fee payments: check the screenshot and reference, then approve or decline. */
@Composable
fun PendingPaymentsScreen(onBack: () -> Unit, onOpenReceipt: (String) -> Unit) {
    val extras = rememberListExtras("money-pending")
    val vm = rememberPaged("money-pending") { page, q, filter ->
        val r = service<MoneyApi>().pending(filter ?: "enrollment_pending", page, q.ifBlank { null })
        extras.value.value = r.counts
        Paged(r.data, r.totalCount, r.page, r.pageSize)
    }
    ReloadOnReturn(vm::refresh)
    val runner = rememberActionRunner()
    val state by vm.state.collectAsStateWithLifecycle()
    val countsAny by extras.value.collectAsStateWithLifecycle()
    @Suppress("UNCHECKED_CAST") val counts = countsAny as? Map<String, Int> ?: emptyMap()
    var confirming by remember { mutableStateOf<Pair<MoneyRow, Boolean>?>(null) }

    ManageScaffold("Pending payments", onBack, runner.snackbar) { padding ->
        PagedListBody(
            vm = vm,
            searchHint = "Name, e-mail, course or reference",
            emptyTitle = if (state.query.isNotBlank()) "No payments match your search." else "No payments found for this category.",
            key = { it.id },
            modifier = Modifier.padding(padding),
            header = {
                item {
                    Text("Verify enrollment and fee payments submitted by students awaiting approval.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    FilterChips(
                        listOf(
                            FilterOption(null, "Enrollment Fee Pending" + (counts["enrollment_pending"]?.takeIf { it > 0 }?.let { " ($it)" } ?: "")),
                            FilterOption("monthly_pending", "Course Fee Pending" + (counts["monthly_pending"]?.takeIf { it > 0 }?.let { " ($it)" } ?: "")),
                        ),
                        state.filter, vm::filter,
                    )
                }
            },
        ) { row ->
            SubmissionCard(row, runner, onOpenReceipt, onChanged = vm::refresh) { approve -> confirming = row to approve }
        }
    }

    confirming?.let { (row, approve) ->
        if (approve) ConfirmDialog(
            "Confirm payment", "Approve this payment of ${money(row.amountInrPaise)} from ${row.user?.display} and notify the student?", "Confirm",
            onConfirm = {
                runner.run({ service<MoneyApi>().approve(row.id) }, success = "Payment approved and recorded. The student will be notified.") { vm.refresh() }
            },
            onDismiss = { confirming = null },
        ) else ConfirmDialog(
            "Decline payment", "Decline this payment and ask the student to resubmit? They will be notified.", "Decline", destructive = true,
            onConfirm = {
                runner.run({ service<MoneyApi>().decline(row.id) }, success = "Payment declined. The student has been notified and can resubmit.") { vm.refresh() }
            },
            onDismiss = { confirming = null },
        )
    }
}

/**
 * A fee submission as PaymentApprovalsTable shows it. Pending: approve / decline ([onDecide]). Approved: the
 * receipt and Void.
 */
@Composable
fun SubmissionCard(row: MoneyRow, runner: ActionRunner, onOpenReceipt: (String) -> Unit, onChanged: () -> Unit, onDecide: (approve: Boolean) -> Unit) {
    var voiding by remember { mutableStateOf(false) }
    val approved = row.status.equals("approved", ignoreCase = true)
    AppCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(row.user?.display ?: "—", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (row.user?.name != null) Text(row.user.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(money(row.amountInrPaise), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            InfoLine("Course", row.courseTitle)
            InfoLine("Description", row.label)
            InfoLine("Method", row.paymentMethod?.replaceFirstChar { it.uppercase() } ?: "—")
            InfoLine("Reference", row.reference ?: "—")
            InfoLine("Submitted", formatDate(row.submittedAt))
        }
        if (!row.screenshotPath.isNullOrBlank()) { Spacer(Modifier.height(8.dp)); ScreenshotThumb(row.screenshotPath, row.screenshotIsPdf) }
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (!approved) {
                Button(onClick = { onDecide(true) }, enabled = !runner.busy) { Icon(Icons.Rounded.CheckCircle, null); Spacer(Modifier.padding(3.dp)); Text("Confirm") }
                OutlinedButton(onClick = { onDecide(false) }, enabled = !runner.busy) { Text("Decline", color = MaterialTheme.colorScheme.error) }
            } else {
                ReceiptButton(row.paymentRecordId, row.receiptGeneratedAt, runner, onOpenReceipt, onChanged)
                TextButton(onClick = { voiding = true }, enabled = !runner.busy) { Text("Void", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
    if (voiding) TextInputDialog(
        "Void this payment?", "Reason for voiding", "Void",
        message = "The payment record is kept for the audit log but no longer counts as income.",
        required = true, destructive = true,
        onConfirm = { reason -> runner.run({ service<MoneyApi>().voidPayment(row.id, reasonForm(reason)) }, success = "Payment voided.") { onChanged() } },
        onDismiss = { voiding = false },
    )
}

// ------------------------------------------------- /admin/completed-payments

private val completedTabs = listOf(
    FilterOption(null, "Enrollment Fee Approved"),
    FilterOption("monthly_approved", "Course Fee Approved"),
    FilterOption("enrollment_waiver", "100% Waiver (Enrollment)"),
    FilterOption("monthly_waiver", "100% Waiver (Course)"),
    FilterOption("book_approved", "Book Order Payments"),
    FilterOption("book_refunded", "Book Order Refunds"),
    FilterOption("manual_transactions", "Manual Transactions"),
    FilterOption("manual_expenses", "Manual Expenses"),
)

/** Every completed payment by tab: approved fees and waivers, book orders, manual transactions and expenses. */
@Composable
fun CompletedPaymentsScreen(onBack: () -> Unit, onOpenReceipt: (String) -> Unit, onOpenReceipts: () -> Unit) {
    val vm = rememberPaged("money-completed") { page, q, filter ->
        val r = service<MoneyApi>().completed(filter ?: "enrollment_approved", page, q.ifBlank { null })
        Paged(r.data, r.totalCount, r.page, r.pageSize)
    }
    ReloadOnReturn(vm::refresh)
    val runner = rememberActionRunner()
    val state by vm.state.collectAsStateWithLifecycle()

    ManageScaffold("Completed payments", onBack, runner.snackbar, actions = {
        IconButton(onClick = onOpenReceipts) { Icon(Icons.Rounded.Receipt, "Payment receipts") }
    }) { padding ->
        PagedListBody(
            vm = vm,
            searchHint = "Name, e-mail, course or reference",
            emptyTitle = if (state.query.isNotBlank()) "Nothing matches your search." else "No payments found for this category.",
            key = { it.kind + it.id },
            modifier = Modifier.padding(padding),
            header = { item { FilterChips(completedTabs, state.filter, vm::filter) } },
        ) { row ->
            when (row.kind) {
                "submission" -> SubmissionCard(row, runner, onOpenReceipt, vm::refresh) { }
                "book_order" -> BookOrderPaymentCard(row, runner, onOpenReceipt, vm::refresh)
                "income" -> IncomeCard(row, runner, onOpenReceipt, vm::refresh, canVoid = true)
                "expense" -> ExpenseCard(row, runner, vm::refresh)
            }
        }
    }
}

/** A book order's payment (BookOrdersTable in payment-only mode). */
@Composable
fun BookOrderPaymentCard(row: MoneyRow, runner: ActionRunner, onOpenReceipt: (String) -> Unit, onChanged: () -> Unit) {
    var voiding by remember { mutableStateOf(false) }
    AppCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(row.user?.display ?: "—", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(listOfNotNull(row.user?.email?.takeIf { row.user.name != null }, formatDate(row.createdAt)).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(money(row.amountInrPaise), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(6.dp))
        moneyStatus(row.status, row.statusLabel).let { StatusChip(it.first, it.second) }
        Spacer(Modifier.height(6.dp))
        row.items.forEach { Text("${it.title} × ${it.quantity} — ${money(it.lineTotalPaise)}", style = MaterialTheme.typography.bodySmall) }
        Spacer(Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            InfoLine("Address", row.deliveryAddress ?: "No address provided")
            InfoLine("PIN", row.deliveryPinCode)
            InfoLine("Phone", row.deliveryPhoneNumber)
            InfoLine("Method", row.paymentMethod?.replaceFirstChar { it.uppercase() } ?: "—")
            InfoLine("Reference", row.reference)
            InfoLine("Courier", row.courierServiceName)
            InfoLine("Tracking ID", row.trackingId)
        }
        if (!row.screenshotPath.isNullOrBlank()) { Spacer(Modifier.height(8.dp)); ScreenshotThumb(row.screenshotPath, row.screenshotIsPdf) }
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReceiptButton(row.paymentRecordId, row.receiptGeneratedAt, runner, onOpenReceipt, onChanged)
            if (row.canVoid) TextButton(onClick = { voiding = true }, enabled = !runner.busy) { Text("Void", color = MaterialTheme.colorScheme.error) }
        }
    }
    if (voiding) TextInputDialog(
        "Void this order?", "Reason for voiding", "Void",
        message = "It stays on record for auditing but disappears from order lists and finance.",
        required = true, destructive = true,
        onConfirm = { reason -> runner.run({ service<BookstoreAdminApi>().orderAction(row.id, "void", reasonForm(reason)) }, success = "Order voided.") { onChanged() } },
        onDismiss = { voiding = false },
    )
}

/** A payment record (income): finance lists and manually added transactions. */
@Composable
fun IncomeCard(row: MoneyRow, runner: ActionRunner?, onOpenReceipt: (String) -> Unit, onChanged: () -> Unit, canVoid: Boolean) {
    var voiding by remember { mutableStateOf(false) }
    AppCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(row.user?.display ?: "—", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(listOfNotNull(formatDate(row.paidAt), row.paymentTypeLabel).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(money(row.amountInrPaise), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Brand.success)
        }
        Spacer(Modifier.height(4.dp))
        InfoLine("Course", row.courseTitle ?: "—")
        InfoLine("Description", row.description)
        if (runner != null) {
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReceiptButton(row.paymentRecordId, row.receiptGeneratedAt, runner, onOpenReceipt, onChanged)
                if (canVoid) TextButton(onClick = { voiding = true }, enabled = !runner.busy) { Text("Void", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
    if (voiding && runner != null) TextInputDialog(
        "Void this payment record?", "Reason for voiding", "Void",
        message = "It is kept for the audit log and left out of finance.",
        required = true, destructive = true,
        onConfirm = { reason -> runner.run({ service<MoneyApi>().voidRecord(row.id, reasonForm(reason)) }, success = "Payment record voided.") { onChanged() } },
        onDismiss = { voiding = false },
    )
}

/** An expense, with Delete. */
@Composable
fun ExpenseCard(row: MoneyRow, runner: ActionRunner, onChanged: () -> Unit) {
    var deleting by remember { mutableStateOf(false) }
    AppCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(row.categoryLabel ?: row.category.orEmpty(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (!row.note.isNullOrBlank()) Text(row.note, style = MaterialTheme.typography.bodySmall)
                Text(formatDate(row.paidAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(money(row.amountInrPaise), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Brand.danger)
        }
        Spacer(Modifier.height(4.dp))
        InfoLine("Teacher", row.teacher?.name)
        InfoLine("Description", row.description)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { deleting = true }, enabled = !runner.busy) { Text("Delete", color = MaterialTheme.colorScheme.error) }
        }
    }
    if (deleting) ConfirmDialog(
        "Delete this expense?", "${row.categoryLabel ?: row.category ?: "This expense"} of ${money(row.amountInrPaise)} on ${formatDate(row.paidAt)} is removed from finance. This cannot be undone.", "Delete",
        destructive = true,
        onConfirm = { runner.run({ service<MoneyApi>().deleteExpense(row.id) }, success = "Expense deleted.") { onChanged() } },
        onDismiss = { deleting = false },
    )
}

/** "Pending" etc. as a chip with the right tone. */
@Composable
fun StatusLine(status: String?, label: String? = null) {
    val (text, tone) = moneyStatus(status, label)
    StatusChip(text, if (tone == Tone.Neutral && status == null) Tone.Neutral else tone)
}
