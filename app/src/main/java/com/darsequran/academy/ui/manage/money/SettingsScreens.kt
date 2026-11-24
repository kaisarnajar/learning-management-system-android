package com.darsequran.academy.ui.manage.money

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.LocalShipping
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.darsequran.academy.data.staff.formOf
import com.darsequran.academy.data.staff.money.MoneyApi
import com.darsequran.academy.data.staff.money.ShippingSlab
import com.darsequran.academy.ui.common.Load
import com.darsequran.academy.ui.common.PrimaryButton
import com.darsequran.academy.ui.common.ReloadOnReturn
import com.darsequran.academy.ui.manage.common.ConfirmDialog
import com.darsequran.academy.ui.manage.common.DetailCard
import com.darsequran.academy.ui.manage.common.FormMoneyField
import com.darsequran.academy.ui.manage.common.FormNumberField
import com.darsequran.academy.ui.manage.common.FormScreen
import com.darsequran.academy.ui.manage.common.FormSection
import com.darsequran.academy.ui.manage.common.FormSwitch
import com.darsequran.academy.ui.manage.common.FormTextField
import com.darsequran.academy.ui.manage.common.LoadedColumn
import com.darsequran.academy.ui.manage.common.ManageScaffold
import com.darsequran.academy.ui.manage.common.RecordCard
import com.darsequran.academy.ui.manage.common.RowIcon
import com.darsequran.academy.ui.manage.common.rememberActionRunner
import com.darsequran.academy.ui.manage.common.rememberLoader

/** /admin/payment-settings: UPI and bank details shown to students when they pay, and the receipt / waiver switches. */
@Composable
fun PaymentSettingsScreen(onBack: () -> Unit) {
    val loader = rememberLoader("money-settings") { service<MoneyApi>().settings().settings }
    val runner = rememberActionRunner()
    val state by loader.state.collectAsStateWithLifecycle()
    val loaded = (state as? Load.Ready)?.value
    var ready by rememberSaveable { mutableStateOf(false) }
    var upiId by rememberSaveable { mutableStateOf("") }
    var upiNumber by rememberSaveable { mutableStateOf("") }
    var payee by rememberSaveable { mutableStateOf("") }
    var accountName by rememberSaveable { mutableStateOf("") }
    var bankName by rememberSaveable { mutableStateOf("") }
    var accountNumber by rememberSaveable { mutableStateOf("") }
    var ifsc by rememberSaveable { mutableStateOf("") }
    var branch by rememberSaveable { mutableStateOf("") }
    var gst by rememberSaveable { mutableStateOf(false) }
    var waivers by rememberSaveable { mutableStateOf(false) }
    var autoApprove by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(loaded) {
        if (loaded != null && !ready) {
            upiId = loaded.upiId; upiNumber = loaded.upiNumber; payee = loaded.upiPayeeName
            accountName = loaded.bankAccountName; bankName = loaded.bankName; accountNumber = loaded.bankAccountNumber
            ifsc = loaded.bankIfsc; branch = loaded.bankBranch
            gst = loaded.includeGstByDefault; waivers = loaded.feeWaiverEnabled; autoApprove = loaded.autoApproveFullWaivers
            ready = true
        }
    }

    FormScreen(
        "Payment details", onBack, runner.busy, snackbar = runner.snackbar, saveLabel = "Save payment details",
        canSave = ready, loadError = (state as? Load.Failed)?.message, onRetry = { loader.reload(true) },
        onSave = {
        runner.run({
            service<MoneyApi>().saveSettings(formOf {
                text("upiId", upiId); text("upiNumber", upiNumber); text("upiPayeeName", payee)
                text("bankAccountName", accountName); text("bankName", bankName); text("bankAccountNumber", accountNumber)
                text("bankIfsc", ifsc); text("bankBranch", branch)
                check("includeGstByDefault", gst); check("feeWaiverEnabled", waivers); check("autoApproveFullWaivers", autoApprove)
            })
        }, success = "Payment details saved.") { loader.reload() }
    }) {
        if (!ready) { Text("Loading…"); return@FormScreen }
        Text(
            "UPI and bank information shown to students when paying course fees. Configure these before students enroll or pay monthly fees.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FormSection("UPI", "Shown on student payment pages and used for QR codes.") {
            FormTextField("UPI ID", upiId, { upiId = it }, supporting = "yourname@bank", capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.None)
            FormTextField("UPI Number (optional)", upiNumber, { upiNumber = it }, supporting = "e.g. 9876543210", keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone)
            FormTextField("Payee name", payee, { payee = it })
        }
        FormSection("Bank transfer", "Displayed alongside UPI for NEFT / IMPS / RTGS.") {
            FormTextField("Account name", accountName, { accountName = it })
            FormTextField("Bank name", bankName, { bankName = it })
            FormTextField("Account number", accountNumber, { accountNumber = it }, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            FormTextField("IFSC code", ifsc, { ifsc = it.uppercase() }, capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Characters)
            FormTextField("Branch (optional)", branch, { branch = it })
        }
        FormSection("Receipt Settings", "Configure defaults for newly generated payment receipts.") {
            FormSwitch("Include GST (18% inclusive) by default in generated receipts", gst, { gst = it })
            FormSwitch("Enable Fee Waiver & Coupon System", waivers, { waivers = it })
            FormSwitch("Auto-approve 100% fee waivers", autoApprove, { autoApprove = it }, "Skip admin confirmation for fully waived fees")
        }
    }
}

/** /admin/shipping-charges: weight slabs and their charges; add and delete. */
@Composable
fun ShippingChargesScreen(onBack: () -> Unit) {
    val loader = rememberLoader("money-shipping") { service<MoneyApi>().slabs().slabs }
    ReloadOnReturn { loader.reload() }
    val runner = rememberActionRunner()
    var deleting by remember { mutableStateOf<ShippingSlab?>(null) }
    var min by rememberSaveable { mutableStateOf("") }
    var max by rememberSaveable { mutableStateOf("") }
    var charge by rememberSaveable { mutableStateOf("") }

    ManageScaffold("Shipping charges", onBack, runner.snackbar) { padding ->
        LoadedColumn(loader, padding) { slabs ->
            item {
                Text(
                    "Configure shipping charges based on order weight. Slabs must be contiguous starting from 0 g. An order heavier than the highest slab pays the highest slab's charge; an order lighter than the lowest slab (for example books without a recorded weight) pays the lowest slab's charge.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item { Text("CURRENT SLABS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (slabs.isEmpty()) item { Text("No shipping slabs configured yet.", style = MaterialTheme.typography.bodyMedium) }
            slabs.forEach { slab ->
                item(key = slab.id) {
                    RecordCard(
                        title = "${slab.minWeightGrams} g – ${slab.maxWeightGrams} g",
                        subtitle = rupeesExact(slab.chargeInrPaise),
                        leading = { RowIcon(Icons.Rounded.LocalShipping) },
                        trailing = { IconButton(onClick = { deleting = slab }) { Icon(Icons.Rounded.Delete, "Delete slab", tint = MaterialTheme.colorScheme.error) } },
                    )
                }
            }
            item {
                DetailCard("Add new slab") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FormNumberField("Min weight (g)", min, { min = it }, Modifier.weight(1f))
                        FormNumberField("Max weight (g)", max, { max = it }, Modifier.weight(1f))
                    }
                    FormMoneyField("Charge (₹)", charge, { charge = it })
                    PrimaryButton("Add slab", {
                        runner.run({
                            service<MoneyApi>().addSlab(formOf { text("minWeightGrams", min); text("maxWeightGrams", max); text("chargeInr", charge) })
                        }, success = "Shipping slab added.") { min = ""; max = ""; charge = ""; loader.reload() }
                    }, enabled = min.isNotBlank() && max.isNotBlank() && charge.isNotBlank(), loading = runner.busy)
                }
            }
        }
    }
    deleting?.let { slab ->
        ConfirmDialog(
            "Delete slab ${slab.minWeightGrams}-${slab.maxWeightGrams}g?", "Orders in this weight range would no longer have a matching slab. Delete slabs from the top down, or add a replacement first.", "Delete",
            destructive = true,
            onConfirm = { runner.run({ service<MoneyApi>().deleteSlab(slab.id) }, success = "Shipping slab deleted.") { loader.reload() } },
            onDismiss = { deleting = null },
        )
    }
}

/** ₹ with paise always shown ("₹50.00"), as the shipping page prints charges. */
private fun rupeesExact(paise: Int): String = "₹" + java.text.NumberFormat.getNumberInstance(java.util.Locale("en", "IN")).apply { minimumFractionDigits = 2; maximumFractionDigits = 2 }.format(paise / 100.0)

