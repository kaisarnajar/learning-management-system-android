package com.darsequran.academy.ui.pay

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.darsequran.academy.data.ApiResult
import com.darsequran.academy.data.remote.PaymentQuote
import com.darsequran.academy.data.remote.PaymentSettings
import com.darsequran.academy.data.repo.AcademyRepository
import com.darsequran.academy.ui.common.AppCard
import com.darsequran.academy.ui.common.BackTopBar
import com.darsequran.academy.ui.common.Banner
import com.darsequran.academy.ui.common.ErrorState
import com.darsequran.academy.ui.common.LoadingBox
import com.darsequran.academy.ui.common.MessageState
import com.darsequran.academy.ui.common.Overline
import com.darsequran.academy.ui.common.PrimaryButton
import com.darsequran.academy.ui.common.SecondaryButton
import com.darsequran.academy.ui.common.Tone
import com.darsequran.academy.ui.common.rupees
import com.darsequran.academy.ui.common.rememberProofImagePicker
import com.darsequran.academy.ui.common.savedVmFactory
import com.darsequran.academy.ui.theme.Brand
import com.darsequran.academy.ui.common.academyToday
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

data class PayState(
    val loading: Boolean = true,
    val error: String? = null,
    val quote: PaymentQuote? = null,
    val settings: PaymentSettings? = null,
    val couponId: String? = null,
    val month: Int = academyToday().monthValue,
    val year: String = academyToday().year.toString(),
    val submitting: Boolean = false,
    val submitError: String? = null,
    val done: Boolean = false,
) {
    val amountPaise: Int get() = quote?.coupons?.firstOrNull { it.id == couponId }?.amountPaise ?: (quote?.baseAmountPaise ?: 0)
}

/**
 * The fee payment flow. The waiver, month and year chosen live in [saved] too: paying sends the student to their UPI
 * app, and if the phone kills this app meanwhile they must come back to the same choices, or the transaction id they
 * paid with would be submitted for another month or amount.
 */
class PayViewModel(
    private val repo: AcademyRepository,
    val courseId: String,
    val type: String,
    private val saved: SavedStateHandle,
) : ViewModel() {
    private val _state = MutableStateFlow(
        PayState(
            couponId = saved[COUPON],
            month = saved[MONTH] ?: academyToday().monthValue,
            year = saved[YEAR] ?: academyToday().year.toString(),
        ),
    )
    val state: StateFlow<PayState> = _state.asStateFlow()

    init { load() }

    fun load() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        val quote = async { repo.paymentQuote(courseId, type) }
        val settings = async { repo.paymentSettings() }
        when (val q = quote.await()) {
            is ApiResult.Ok -> _state.update {
                it.copy(
                    loading = false,
                    quote = q.value,
                    settings = (settings.await() as? ApiResult.Ok)?.value,
                    year = if (saved.contains(YEAR)) it.year
                    else q.value.yearOptions.firstOrNull { y -> y == academyToday().year.toString() } ?: q.value.yearOptions.firstOrNull() ?: it.year,
                )
            }
            is ApiResult.Err -> _state.update { it.copy(loading = false, error = q.message) }
        }
    }

    fun selectCoupon(id: String?) { saved[COUPON] = id; _state.update { it.copy(couponId = id) } }
    fun selectMonth(m: Int) { saved[MONTH] = m; _state.update { it.copy(month = m) } }
    fun selectYear(y: String) { saved[YEAR] = y; _state.update { it.copy(year = y) } }

    private companion object {
        const val COUPON = "couponId"
        const val MONTH = "month"
        const val YEAR = "year"
    }

    /** One submission per payment: a second tap (even in the same frame) or a tap after success is ignored. */
    fun submit(method: String, reference: String, screenshot: Pair<ByteArray, String>?) {
        val s = _state.value
        if (s.submitting || s.done) return
        val quote = s.quote ?: return
        _state.update { it.copy(submitting = true, submitError = null) }
        viewModelScope.launch { send(s, quote, method, reference, screenshot) }
    }

    private suspend fun send(s: PayState, quote: PaymentQuote, method: String, reference: String, screenshot: Pair<ByteArray, String>?) {
        val fields = buildMap {
            put("courseId", courseId)
            put("paymentType", if (type == "enrollment") "enrollment" else "monthly")
            if (type == "course") {
                put("paymentMonth", if (quote.oneTimeFee) "01" else s.month.toString().padStart(2, '0'))
                put("paymentYear", s.year)
            }
            if (s.amountPaise > 0) {
                put("paymentMethod", method)
                put("upiTransactionId", reference.trim())
            }
            put("couponId", s.couponId ?: "none")
            put("expectedAmountPaise", s.amountPaise.toString())
        }
        when (val r = repo.submitPayment(fields, screenshot)) {
            is ApiResult.Ok -> _state.update { it.copy(submitting = false, done = true) }
            is ApiResult.Err -> _state.update { it.copy(submitting = false, submitError = r.message) }
        }
    }
}

/** Reads a picked image and re-encodes it as a JPEG no wider than 1600px (keeps uploads small). */
suspend fun readImage(context: Context, uri: Uri): Pair<ByteArray, String>? = withContext(Dispatchers.IO) {
    runCatching {
        val bitmap = context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it) } ?: return@runCatching null
        val scale = minOf(1f, 1600f / maxOf(bitmap.width, bitmap.height))
        val scaled = if (scale < 1f) Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true) else bitmap
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
        out.toByteArray() to "image/jpeg"
    }.getOrNull()
}

/** Opens the payer's UPI app with payee, amount and note filled in. */
fun launchUpi(context: Context, settings: PaymentSettings, amountPaise: Int, note: String): Boolean {
    val vpa = settings.upiId?.takeIf { it.isNotBlank() } ?: return false
    val uri = Uri.Builder().scheme("upi").authority("pay")
        .appendQueryParameter("pa", vpa)
        .appendQueryParameter("pn", settings.upiPayeeName ?: "Darse Quran Academy")
        .appendQueryParameter("am", "%.2f".format(Locale.ROOT, amountPaise / 100.0))
        .appendQueryParameter("cu", "INR")
        .appendQueryParameter("tn", note.take(60))
        .build()
    return runCatching { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW, uri), "Pay with")) }.isSuccess
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayScreen(
    courseId: String,
    type: String,
    title: String,
    onBack: () -> Unit,
    onDone: () -> Unit,
    vm: PayViewModel = viewModel(key = "pay-$courseId-$type", factory = savedVmFactory { c, saved -> PayViewModel(c.repo, courseId, type, saved) }),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val heading = if (type == "enrollment") "Enrollment fee" else "Course fee"

    Scaffold(topBar = { BackTopBar(heading, onBack) }) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                state.loading -> LoadingBox()
                state.error != null -> ErrorState(state.error!!, vm::load)
                state.done -> MessageState(
                    Icons.Rounded.CheckCircle,
                    if (state.amountPaise == 0) "Waiver submitted" else "Payment submitted",
                    "The academy will verify it and notify you. You can follow it under Account → Payments.",
                    "Done",
                    onDone,
                )
                state.quote?.canPay == false -> MessageState(Icons.Rounded.Info, "Payment not available", state.quote?.reason, "Back", onBack)
                else -> PayFlow(state, vm, title, heading)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PayFlow(state: PayState, vm: PayViewModel, courseTitle: String, heading: String) {
    val context = LocalContext.current
    val quote = state.quote!!
    var method by rememberSaveable { mutableStateOf("upi") }
    var reference by rememberSaveable { mutableStateOf("") }
    val proof = rememberProofImagePicker()
    val free = state.amountPaise == 0
    val settings = state.settings

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // ---- step 1: amount
        StepHeader(1, "Amount")
        AppCard(Modifier.fillMaxWidth()) {
            Text(courseTitle, style = MaterialTheme.typography.titleMedium)
            Text(heading + (quote.feeFrequencyLabel?.let { " · $it" } ?: ""), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Text(rupees(state.amountPaise), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
            if (state.couponId != null) Text("Standard fee ${rupees(quote.baseAmountPaise)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (quote.coupons.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Overline("Fee waiver")
                ChoiceRow("No waiver · ${rupees(quote.baseAmountPaise)}", state.couponId == null) { vm.selectCoupon(null) }
                quote.coupons.forEach { c -> ChoiceRow("${c.code} · ${c.percentage}% off · ${rupees(c.amountPaise)}", state.couponId == c.id) { vm.selectCoupon(c.id) } }
            }

            if (vm.type == "course" && !quote.oneTimeFee) {
                Spacer(Modifier.height(14.dp))
                Overline("Paying for")
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Dropdown(
                        Month.of(state.month).getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                        (1..12).map { Month.of(it).getDisplayName(TextStyle.FULL, Locale.ENGLISH) },
                        Modifier.weight(1.4f),
                    ) { vm.selectMonth(it + 1) }
                    Dropdown(state.year, quote.yearOptions, Modifier.weight(1f)) { vm.selectYear(quote.yearOptions[it]) }
                }
            }
        }

        if (!free) {
            // ---- step 2: pay
            StepHeader(2, "Pay")
            if (settings?.upiId.isNullOrBlank().not()) {
                PrimaryButton("Pay ${rupees(state.amountPaise)} with a UPI app", {
                    if (!launchUpi(context, settings!!, state.amountPaise, "$heading - $courseTitle")) {
                        android.widget.Toast.makeText(context, "No UPI app found. Use the details below.", android.widget.Toast.LENGTH_LONG).show()
                    }
                })
            }
            AppCard(Modifier.fillMaxWidth()) {
                if (!settings?.upiId.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.QrCode2, null, tint = Brand.gold)
                        Spacer(Modifier.width(10.dp)); Overline("UPI")
                    }
                    CopyRow("UPI ID", settings?.upiId)
                    CopyRow("UPI number", settings?.upiNumber)
                    CopyRow("Payee", settings?.upiPayeeName, copy = false)
                }
                if (!settings?.bankAccountNumber.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AccountBalance, null, tint = Brand.gold)
                        Spacer(Modifier.width(10.dp)); Overline("Bank transfer")
                    }
                    CopyRow("Account name", settings?.bankAccountName, copy = false)
                    CopyRow("Account number", settings?.bankAccountNumber)
                    CopyRow("IFSC", settings?.bankIfsc)
                    CopyRow("Bank", listOfNotNull(settings?.bankName, settings?.bankBranch).joinToString(", "), copy = false)
                }
            }

            // ---- step 3: confirm
            StepHeader(3, "Confirm")
            val methods = listOfNotNull("upi" to "UPI", ("bank" to "Bank transfer").takeIf { !settings?.bankAccountNumber.isNullOrBlank() })
            // Only ask how they paid when there is more than one way to pay.
            if (methods.size > 1) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                methods.forEach { (value, label) ->
                    ChoicePill(label, method == value, Modifier.weight(1f)) { method = value }
                }
            }
            OutlinedTextField(
                value = reference,
                onValueChange = { v -> reference = v.filter { it.isLetterOrDigit() }.take(40) },
                label = { Text("Transaction reference (UTR)") },
                supportingText = { Text("The 12-digit reference shown in your UPI or bank app after paying.") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, capitalization = KeyboardCapitalization.Characters),
            )
            ScreenshotPicker(proof.image?.preview, onPick = proof::pick, onClear = proof::clear)
        } else {
            Banner("This fee is fully waived. Submit it for the academy to confirm.", Tone.Success)
        }

        state.submitError?.let { Banner(it, Tone.Danger) }
        PrimaryButton(
            if (free) "Submit waiver" else "Submit payment of ${rupees(state.amountPaise)}",
            onClick = { vm.submit(method, reference, proof.image?.upload) },
            enabled = free || reference.length >= 6,
            loading = state.submitting,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StepHeader(number: Int, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
            Text("$number", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary)
        }
        Spacer(Modifier.width(10.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected, onClick)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun ChoicePill(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = MaterialTheme.shapes.medium
    Box(
        modifier.clip(shape)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else Brand.outlineSoft, shape)
            .clickable(onClick = onClick).padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface) }
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
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            IconButton({
                scope.launch { clipboard.setClipEntry(androidx.compose.ui.platform.ClipEntry(ClipData.newPlainText(label, value))) }
                android.widget.Toast.makeText(context, "$label copied", android.widget.Toast.LENGTH_SHORT).show()
            }) { Icon(Icons.Rounded.ContentCopy, "Copy $label", tint = MaterialTheme.colorScheme.primary) }
        }
    }
}

@Composable
fun ScreenshotPicker(preview: Bitmap?, onPick: () -> Unit, onClear: () -> Unit) {
    if (preview == null) {
        SecondaryButton("Attach payment screenshot (optional)", onPick)
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(preview.asImageBitmap(), "Payment screenshot", Modifier.size(72.dp).clip(MaterialTheme.shapes.medium), contentScale = ContentScale.Crop)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Screenshot attached", style = MaterialTheme.typography.titleSmall)
                Row {
                    TextButton(onPick) { Icon(Icons.Rounded.AddPhotoAlternate, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Change") }
                    TextButton(onClear) { Text("Remove") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Dropdown(value: String, options: List<String>, modifier: Modifier = Modifier, label: String? = null, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded, { expanded = it }, modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = label?.let { { Text(it) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            singleLine = true,
        )
        ExposedDropdownMenu(expanded, { expanded = false }) {
            options.forEachIndexed { i, option -> DropdownMenuItem({ Text(option) }, { onSelect(i); expanded = false }) }
        }
    }
}
