package com.darsequran.academy.ui.pay

import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.darsequran.academy.data.ApiResult
import com.darsequran.academy.data.repo.orNull
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
import com.darsequran.academy.ui.common.Tone
import com.darsequran.academy.ui.common.rupees
import com.darsequran.academy.ui.common.rememberProofImagePicker
import com.darsequran.academy.ui.common.savedVmFactory
import com.darsequran.academy.ui.common.academyToday
import com.darsequran.academy.ui.common.Dropdown
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    val settingsError: String? = null,
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
                    settings = settings.await().orNull(),
                    settingsError = (settings.await() as? ApiResult.Err)?.message,
                    year = if (saved.contains(YEAR)) it.year
                    else q.value.yearOptions.firstOrNull { y -> y == academyToday().year.toString() } ?: q.value.yearOptions.firstOrNull() ?: it.year,
                )
            }
            is ApiResult.Err -> _state.update { it.copy(loading = false, error = q.message) }
        }
    }

    /** Fetches the academy's UPI and bank details again (after they failed to load). */
    fun reloadSettings() = viewModelScope.launch {
        _state.update { it.copy(settingsError = null) }
        val r = repo.paymentSettings()
        _state.update { it.copy(settings = r.orNull() ?: it.settings, settingsError = (r as? ApiResult.Err)?.message) }
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
                    PAYMENT_SUBMITTED_TITLE,
                    PAYMENT_SUBMITTED_BODY,
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
                Overline("Available Coupons & Fee Waivers")
                ChoiceRow("$NO_COUPON · ${rupees(quote.baseAmountPaise)}", state.couponId == null) {
                    vm.selectCoupon(null)
                }
                quote.coupons.forEach { c -> ChoiceRow("${c.code} · ${c.percentage}% off · ${rupees(c.amountPaise)}", state.couponId == c.id) { vm.selectCoupon(c.id) } }
            }

            if (vm.type == "course" && !quote.oneTimeFee) {
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Dropdown(
                        Month.of(state.month).getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                        (1..12).map { Month.of(it).getDisplayName(TextStyle.FULL, Locale.ENGLISH) },
                        Modifier.weight(1.4f),
                        label = "Starting Month",
                    ) { vm.selectMonth(it + 1) }
                    Dropdown(state.year, quote.yearOptions, Modifier.weight(1f), label = "Starting Year") {
                        vm.selectYear(quote.yearOptions[it])
                    }
                }
            }
        }

        if (!free) {
            // ---- step 2: pay
            StepHeader(2, "Pay")
            PaymentDetailsError(state.settingsError, vm::reloadSettings)
            PayWithUpiButton(settings, state.amountPaise, "$heading - $courseTitle")
            val note = feePaymentNote(vm.type, courseTitle, quote.feeFrequencyLabel)
            PaymentDetailsCard(settings, state.amountPaise, note)

            // ---- step 3: confirm
            StepHeader(3, "Confirm")
            PaymentConfirmFields(settings, method, { method = it }, reference, { reference = it }, proof)
        } else {
            Banner("This fee is fully waived. Submit it for the academy to confirm.", Tone.Success)
        }

        state.submitError?.let { Banner(it, Tone.Danger) }
        PrimaryButton(
            paySubmitLabel(vm.type, free),
            onClick = { vm.submit(method, reference, proof.image?.upload) },
            enabled = canSubmitPayment(free, reference),
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


/** The website's toast on the payments page after any fee submission, waivers included, split in two lines. */
internal const val PAYMENT_SUBMITTED_TITLE = "Payment submitted."
internal const val PAYMENT_SUBMITTED_BODY = "The academy will verify it shortly."

/** The website's CouponSelector option for paying the full fee. */
internal const val NO_COUPON = "Do not use any coupon"

private const val NOTE_MAX = 80

/** The website's submit button wording (EnrollmentPaymentForm and MonthlyPaymentForm). */
internal fun paySubmitLabel(type: String, free: Boolean): String = when {
    type == "enrollment" && free -> "Claim Fee Waiver & Enroll"
    type == "enrollment" -> "Submit enrollment fee for verification"
    free -> "Claim Fee Waiver"
    else -> "Submit fee payment for verification"
}

/** The website only requires the transaction reference to be filled in; a waived fee needs none. */
internal fun canSubmitPayment(free: Boolean, reference: String): Boolean = free || reference.isNotBlank()

/** The UPI note the website's pay forms put in the QR code. */
internal fun feePaymentNote(type: String, courseTitle: String, feeFrequencyLabel: String?): String =
    if (type == "enrollment") "$courseTitle enrollment".take(NOTE_MAX)
    else "$courseTitle — ${feeFrequencyLabel ?: "Monthly"}".take(NOTE_MAX)
