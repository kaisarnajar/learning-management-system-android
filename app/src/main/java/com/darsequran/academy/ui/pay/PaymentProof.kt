package com.darsequran.academy.ui.pay

import android.content.ClipData
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.darsequran.academy.data.remote.PaymentSettings
import com.darsequran.academy.ui.common.AppCard
import com.darsequran.academy.ui.common.Banner
import com.darsequran.academy.ui.common.Overline
import com.darsequran.academy.ui.common.PrimaryButton
import com.darsequran.academy.ui.common.ProofImagePicker
import com.darsequran.academy.ui.common.SecondaryButton
import com.darsequran.academy.ui.common.Tone
import com.darsequran.academy.ui.common.rupees
import com.darsequran.academy.ui.theme.Brand
import kotlinx.coroutines.launch

/*
 * The "pay, then tell us how" part shared by fee payments and book checkout, so both show the same details and ask
 * for the transaction reference the same way.
 */

/** Opens the payer's UPI app with the amount filled in (when the academy has a UPI ID). */
@Composable
fun PayWithUpiButton(settings: PaymentSettings?, amountPaise: Int, note: String) {
    if (settings == null || settings.upiId.isNullOrBlank()) return
    val context = LocalContext.current
    PrimaryButton("Pay ${rupees(amountPaise)} with a UPI app", {
        if (!launchUpi(context, settings, amountPaise, note)) {
            Toast.makeText(context, "No UPI app found. Use the details below.", Toast.LENGTH_LONG).show()
        }
    })
}

/** The payment details failed to load: without them there is nothing to pay to, so say so and offer a retry. */
@Composable
fun PaymentDetailsError(message: String?, onRetry: () -> Unit) {
    if (message == null) return
    Banner("Couldn't load the academy's payment details. $message", Tone.Danger)
    SecondaryButton("Try again", onRetry)
}

/** The academy's UPI and bank details, each with a copy button. */
@Composable
fun PaymentDetailsCard(settings: PaymentSettings?) {
    if (settings == null) return
    AppCard(Modifier.fillMaxWidth()) {
        if (!settings.upiId.isNullOrBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.QrCode2, null, tint = Brand.gold)
                Spacer(Modifier.width(10.dp)); Overline("UPI")
            }
            CopyRow("UPI ID", settings.upiId)
            CopyRow("UPI number", settings.upiNumber)
            CopyRow("Payee", settings.upiPayeeName, copy = false)
        }
        if (!settings.bankAccountNumber.isNullOrBlank()) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AccountBalance, null, tint = Brand.gold)
                Spacer(Modifier.width(10.dp)); Overline("Bank transfer")
            }
            CopyRow("Account name", settings.bankAccountName, copy = false)
            CopyRow("Account number", settings.bankAccountNumber)
            CopyRow("IFSC", settings.bankIfsc)
            CopyRow("Bank", listOfNotNull(settings.bankName, settings.bankBranch).joinToString(", "), copy = false)
        }
    }
}

/** How the student paid (asked only when there is more than one way), the transaction reference and the screenshot. */
@Composable
fun PaymentConfirmFields(
    settings: PaymentSettings?,
    method: String,
    onMethod: (String) -> Unit,
    reference: String,
    onReference: (String) -> Unit,
    proof: ProofImagePicker,
) {
    val methods = listOfNotNull("upi" to "UPI", ("bank" to "Bank transfer").takeIf { !settings?.bankAccountNumber.isNullOrBlank() })
    if (methods.size > 1) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        methods.forEach { (value, label) -> ChoicePill(label, method == value, Modifier.weight(1f)) { onMethod(value) } }
    }
    OutlinedTextField(
        value = reference,
        onValueChange = { v -> onReference(v.filter { it.isLetterOrDigit() }.take(40)) },
        label = { Text("Transaction reference (UTR)") },
        supportingText = { Text("The 12-digit reference shown in your UPI or bank app after paying.") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, capitalization = KeyboardCapitalization.Characters),
    )
    ScreenshotPicker(proof.image?.preview, onPick = proof::pick, onClear = proof::clear)
}

@Composable
private fun CopyRow(label: String, value: String?, copy: Boolean = true) {
    if (value.isNullOrBlank()) return
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
        if (copy) {
            val scope = rememberCoroutineScope()
            IconButton({
                scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(label, value))) }
                Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
            }) { Icon(Icons.Rounded.ContentCopy, "Copy $label", tint = MaterialTheme.colorScheme.primary) }
        }
    }
}
