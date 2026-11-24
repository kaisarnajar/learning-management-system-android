package com.darsequran.academy.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.darsequran.academy.data.ApiResult
import com.darsequran.academy.data.remote.Course
import com.darsequran.academy.data.remote.WaiverRequest
import com.darsequran.academy.data.remote.WaiverRequestBody
import com.darsequran.academy.data.repo.AcademyRepository
import com.darsequran.academy.data.repo.orNull
import com.darsequran.academy.ui.common.AppCard
import com.darsequran.academy.ui.common.BackTopBar
import com.darsequran.academy.ui.common.Banner
import com.darsequran.academy.ui.common.Dropdown
import com.darsequran.academy.ui.common.EmptyState
import com.darsequran.academy.ui.common.Load
import com.darsequran.academy.ui.common.LoadContent
import com.darsequran.academy.ui.common.Overline
import com.darsequran.academy.ui.common.PrimaryButton
import com.darsequran.academy.ui.common.StatusChip
import com.darsequran.academy.ui.common.StatusKind
import com.darsequran.academy.ui.common.statusLabel
import com.darsequran.academy.ui.common.Tone
import com.darsequran.academy.ui.common.formatDate
import com.darsequran.academy.ui.common.vmFactory
import com.darsequran.academy.ui.theme.Brand
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** The waiver page: the student's requests and the courses a new request can name. */
data class WaiverPage(val requests: List<WaiverRequest>, val courses: List<Course>)

class WaiversViewModel(private val repo: AcademyRepository) : ViewModel() {
    val state = MutableStateFlow<Load<WaiverPage>>(Load.Loading)
    val sending = MutableStateFlow(false)
    val message = MutableStateFlow<Pair<String, Boolean>?>(null)
    init { load() }

    fun load() = viewModelScope.launch {
        // Like the website, every course open for enrollment, not only the ones applied for.
        val courses = async { waiverCourseOptions(repo.allCourses().orNull().orEmpty()) }
        state.value = when (val r = repo.waiverRequests()) {
            is ApiResult.Ok -> Load.Ready(WaiverPage(r.value, courses.await()))
            is ApiResult.Err -> Load.Failed(r.message)
        }
    }

    /** One request at a time: a second tap before the first is sent (even in the same frame) is ignored. */
    fun request(body: WaiverRequestBody, onDone: () -> Unit) {
        if (sending.value) return
        sending.value = true
        viewModelScope.launch {
            when (val r = repo.requestWaiver(body)) {
                is ApiResult.Ok -> {
                    message.value = WAIVER_SENT to true
                    onDone()
                    load()
                }
                is ApiResult.Err -> message.value = r.message to false
            }
            sending.value = false
        }
    }

    companion object {
        const val WAIVER_SENT = "Your fee waiver request has been submitted successfully!"
    }
}

@Composable
fun WaiversScreen(onBack: () -> Unit) {
    val vm: WaiversViewModel = viewModel(factory = vmFactory { WaiversViewModel(it.repo) })
    val state by vm.state.collectAsStateWithLifecycle()
    val sending by vm.sending.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    var showForm by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        topBar = { BackTopBar("Fee Waiver Requests", onBack) },
        floatingActionButton = {
            if (!showForm) {
                ExtendedFloatingActionButton(
                    onClick = { showForm = true },
                    icon = { Icon(Icons.Rounded.Edit, null) },
                    text = { Text("Request Fee Waiver") },
                )
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            LoadContent(state, vm::load) { page ->
                val form: (@Composable () -> Unit)? = if (!showForm) null else {
                    { WaiverForm(page.courses, sending, { showForm = false }) { vm.request(it) { showForm = false } } }
                }
                WaiverList(page, message, form)
            }
        }
    }
}

@Composable
private fun WaiverList(page: WaiverPage, message: Pair<String, Boolean>?, form: (@Composable () -> Unit)?) {
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(
                "Track and manage your fee waiver requests.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // Keyed: without keys the banner appearing above the form (a failed request) would move the form to another
        // index, and it would start over empty just when the student needs to send it again.
        message?.let { (text, ok) -> item(key = "message") { Banner(text, if (ok) Tone.Success else Tone.Danger) } }
        if (form != null) item(key = "waiver-form") { form() }
        if (page.requests.isEmpty()) {
            item {
                EmptyState(
                    "No Fee Waiver Requests",
                    "When you submit a fee waiver request during course enrollment or payment, your submitted " +
                        "requests and approved coupon status will appear here.",
                )
            }
        } else {
            item { Overline("Past Requests") }
        }
        items(page.requests, key = { it.id }) { WaiverCard(it) }
    }
}

@Composable
private fun WaiverCard(w: WaiverRequest) {
    val (label, tone) = statusLabel(w.status, StatusKind.Waiver)
    AppCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val title = w.course?.title ?: "Course"
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            StatusChip(label, tone)
        }
        Text(
            "${waiverFeeLabel(w.feeType)} · ${formatDate(w.createdAt)}",
            style = MaterialTheme.typography.labelMedium,
            color = Brand.gold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Assigned Coupon",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val coupon = waiverCouponText(w)
        if (coupon == null) {
            Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text(coupon, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Brand.success)
        }
    }
}

@Composable
private fun WaiverForm(
    courses: List<Course>,
    sending: Boolean,
    onCancel: () -> Unit,
    onSend: (WaiverRequestBody) -> Unit,
) {
    var course by rememberSaveable { mutableStateOf("") }
    var fee by rememberSaveable { mutableStateOf(DEFAULT_WAIVER_FEE) }
    var category by rememberSaveable { mutableStateOf(WAIVER_REASONS.first()) }
    var details by rememberSaveable { mutableStateOf("") }
    val other = category == WAIVER_REASON_OTHER
    AppCard(Modifier.fillMaxWidth()) {
        Text("Request Fee Waiver", style = MaterialTheme.typography.titleLarge)
        Text(
            "If you are unable to pay the course fees, you can request a fee waiver here. The administration " +
                "will review your request and may provide you with a special coupon code.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        val courseTitle = courses.firstOrNull { it.id == course }?.title ?: "Select a course..."
        Dropdown(courseTitle, courses.map { it.title }, Modifier.fillMaxWidth(), "Course") { course = courses[it].id }
        Spacer(Modifier.height(10.dp))
        Dropdown(fee.label, WaiverFee.entries.map { it.label }, Modifier.fillMaxWidth(), "Fee Type") {
            fee = WaiverFee.entries[it]
        }
        Spacer(Modifier.height(10.dp))
        Dropdown(category, WAIVER_REASONS, Modifier.fillMaxWidth(), "Reason for Waiver") {
            category = WAIVER_REASONS[it]
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            details,
            { details = it.take(WAIVER_DETAILS_MAX) },
            Modifier.fillMaxWidth().height(120.dp),
            label = { Text(if (other) "Please specify reason" else "Additional details (optional)") },
            placeholder = {
                Text(
                    if (other) "Describe your reason..."
                    else "Add any additional context for your request (optional)...",
                )
            },
            supportingText = { Text("${details.length}/$WAIVER_DETAILS_MAX") },
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(onCancel, Modifier.weight(1f)) { Text("Cancel") }
            PrimaryButton(
                "Submit Request",
                { onSend(waiverRequestBody(course, fee, category, details)) },
                Modifier.weight(2f),
                enabled = waiverFormReady(course, category, details),
                loading = sending,
            )
        }
    }
}
