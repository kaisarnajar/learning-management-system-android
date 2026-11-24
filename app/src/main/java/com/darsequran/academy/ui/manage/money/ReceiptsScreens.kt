package com.darsequran.academy.ui.manage.money

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.darsequran.academy.ui.common.Banner
import com.darsequran.academy.ui.common.PrimaryButton
import com.darsequran.academy.ui.common.ReloadOnReturn
import com.darsequran.academy.ui.common.Tone
import com.darsequran.academy.ui.common.formatDate
import com.darsequran.academy.ui.common.formatDateTime
import com.darsequran.academy.ui.manage.common.ConfirmDialog
import com.darsequran.academy.ui.manage.common.DetailCard
import com.darsequran.academy.ui.manage.common.Field
import com.darsequran.academy.ui.manage.common.FieldRow
import com.darsequran.academy.ui.manage.common.LoadedColumn
import com.darsequran.academy.ui.manage.common.ManageListScreen
import com.darsequran.academy.ui.manage.common.ManageScaffold
import com.darsequran.academy.ui.manage.common.RecordCard
import com.darsequran.academy.ui.manage.common.RowIcon
import com.darsequran.academy.ui.manage.common.rememberActionRunner
import com.darsequran.academy.ui.manage.common.rememberLoader
import com.darsequran.academy.ui.manage.common.rememberPaged

/** /admin/receipts: every generated receipt, searchable, with delete. */
@Composable
fun ReceiptsScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val vm = rememberPaged("money-receipts") { page, q, _ -> service<MoneyApi>().receipts(page, q.ifBlank { null }) }
    ReloadOnReturn(vm::refresh)
    val runner = rememberActionRunner()
    var deleting by remember { mutableStateOf<ReceiptRow?>(null) }
    ManageListScreen(
        title = "Payment receipts",
        onBack = onBack,
        vm = vm,
        key = { it.id },
        emptyTitle = "No receipts have been generated yet.",
        searchHint = "Receipt number, name, payment type or course",
        snackbar = runner.snackbar,
    ) { r ->
        RecordCard(
            title = r.invoiceNumber,
            subtitle = listOfNotNull(r.user?.display, r.paymentTypeLabel.replaceFirstChar { it.uppercase() }, r.courseTitle).joinToString(" · "),
            meta = "${money(r.amountInrPaise)} · ${formatDate(r.receiptGeneratedAt)}",
            chip = if (r.receiptIncludesGst) "GST included" to Tone.Success else "No GST" to Tone.Neutral,
            leading = { RowIcon(Icons.Rounded.Receipt) },
            trailing = { IconButton(onClick = { deleting = r }) { Icon(Icons.Rounded.Delete, "Delete receipt", tint = MaterialTheme.colorScheme.error) } },
            onClick = { onOpen(r.id) },
        )
    }
    deleting?.let { r ->
        ConfirmDialog(
            "Delete receipt ${r.invoiceNumber}?", "The payment stays recorded; a new receipt (with a new number) can be generated later.", "Delete",
            destructive = true,
            onConfirm = { runner.run({ service<MoneyApi>().deleteReceipt(r.id) }, success = "Receipt deleted successfully.") { vm.refresh() } },
            onDismiss = { deleting = null },
        )
    }
}

/** /admin/receipts/[paymentRecordId]: the receipt of one payment: open the PDF, e-mail it, regenerate or delete it. */
@Composable
fun ReceiptDetailScreen(recordId: String, onBack: () -> Unit) {
    val loader = rememberLoader("money-receipt-$recordId") { service<MoneyApi>().receipt(recordId).receipt }
    ReloadOnReturn { loader.reload() }
    val runner = rememberActionRunner()
    val context = LocalContext.current
    var confirm by remember { mutableStateOf<String?>(null) }

    ManageScaffold("Payment receipt", onBack, runner.snackbar) { padding ->
        LoadedColumn(loader, padding) { r ->
            val generated = r.receiptGeneratedAt != null && r.invoiceNumber != null
            item {
                DetailCard(if (generated) r.invoiceNumber else "No receipt yet") {
                    if (r.voided) Banner("This payment has been voided.", Tone.Warning)
                    Field("Student", listOfNotNull(r.user?.name, r.user?.email).joinToString("\n"))
                    Field("Course", r.courseTitle ?: "—")
                    Field("Payment", listOfNotNull(r.paymentTypeLabel, r.description).joinToString(" · "))
                    Field("Paid on", formatDate(r.paidAt))
                    Field("Method", listOfNotNull(r.paymentMethod?.replaceFirstChar { it.uppercase() }, r.reference).joinToString(" · ").ifBlank { null }, showEmpty = false)
                }
            }
            item {
                DetailCard("Amount") {
                    if (generated) {
                        FieldRow(if (r.receiptIncludesGst) "Fee (excl. GST)" else "Fee", money(r.receiptFeeAmountPaise ?: r.amountInrPaise))
                        if (r.receiptIncludesGst) FieldRow("GST (18% inclusive)", money(r.receiptGstAmountPaise ?: 0))
                        if ((r.receiptShippingAmountPaise ?: 0) > 0) FieldRow("Shipping", money(r.receiptShippingAmountPaise ?: 0))
                    }
                    FieldRow("Total", money(r.amountInrPaise), strong = true)
                }
            }
            item {
                if (generated) DetailCard("Receipt") {
                    Field("Generated", formatDateTime(r.receiptGeneratedAt))
                    Field("E-mailed to the student", r.receiptEmailSentAt?.let(::formatDateTime) ?: "Not yet")
                    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedButton(onClick = {
                            runner.openFile("${r.invoiceNumber!!.replace('/', '-')}.pdf", fetch = { service<MoneyApi>().receiptPdf(recordId) }, context = context)
                        }, enabled = !runner.busy) { Icon(Icons.Rounded.PictureAsPdf, null); Spacer(Modifier.width(6.dp)); Text("Open PDF") }
                        OutlinedButton(onClick = {
                            runner.run({ service<MoneyApi>().emailReceipt(recordId) }, success = "Receipt sent. Ask the student to check their Inbox or Spam/Junk folder.") { loader.reload() }
                        }, enabled = !runner.busy) { Icon(Icons.Rounded.Email, null); Spacer(Modifier.width(6.dp)); Text("Send via e-mail") }
                        OutlinedButton(onClick = { confirm = "regenerate" }, enabled = !runner.busy && !r.voided) { Icon(Icons.Rounded.Autorenew, null); Spacer(Modifier.width(6.dp)); Text("Regenerate") }
                        OutlinedButton(onClick = { confirm = "delete" }, enabled = !runner.busy) { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error); Spacer(Modifier.width(6.dp)); Text("Delete", color = MaterialTheme.colorScheme.error) }
                    }
                } else DetailCard("Receipt") {
                    Text("No receipt has been generated for this payment.", style = MaterialTheme.typography.bodyMedium)
                    PrimaryButton("Generate receipt", {
                        runner.run({ service<MoneyApi>().generateReceipt(recordId) }, success = "Receipt generated successfully.") { loader.reload() }
                    }, enabled = !r.voided, loading = runner.busy)
                }
            }
        }
    }

    when (confirm) {
        "delete" -> ConfirmDialog(
            "Delete this receipt?", "The payment stays recorded; a new receipt (with a new number) can be generated later.", "Delete", destructive = true,
            onConfirm = { runner.run({ service<MoneyApi>().deleteReceipt(recordId) }, success = "Receipt deleted successfully.") { loader.reload() } },
            onDismiss = { confirm = null },
        )
        "regenerate" -> ConfirmDialog(
            "Regenerate this receipt?", "The current receipt is deleted and a new one is issued with the current settings and a new receipt number.", "Regenerate",
            onConfirm = {
                runner.run({ service<MoneyApi>().deleteReceipt(recordId); service<MoneyApi>().generateReceipt(recordId) }, success = "Receipt regenerated.") { loader.reload() }
            },
            onDismiss = { confirm = null },
        )
    }
}
