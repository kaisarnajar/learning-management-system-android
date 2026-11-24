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
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.darsequran.academy.data.remote.Paged
import com.darsequran.academy.data.staff.formOf
import com.darsequran.academy.data.staff.money.CouponRow
import com.darsequran.academy.data.staff.money.FinanceRange
import com.darsequran.academy.data.staff.money.MoneyApi
import com.darsequran.academy.data.staff.money.WaiverRequest
import com.darsequran.academy.data.staff.money.WaiverStats
import com.darsequran.academy.ui.common.AppCard
import com.darsequran.academy.ui.common.Load
import com.darsequran.academy.ui.common.PagedListBody
import com.darsequran.academy.ui.common.ReloadOnReturn
import com.darsequran.academy.ui.common.SearchField
import com.darsequran.academy.ui.common.StatusChip
import com.darsequran.academy.ui.common.Tone
import com.darsequran.academy.ui.common.academyToday
import com.darsequran.academy.ui.common.formatCalendarDate
import com.darsequran.academy.ui.common.formatDate
import com.darsequran.academy.ui.manage.common.ActionRunner
import com.darsequran.academy.ui.manage.common.AddFab
import com.darsequran.academy.ui.manage.common.ConfirmDialog
import com.darsequran.academy.ui.manage.common.FilterChips
import com.darsequran.academy.ui.manage.common.FilterOption
import com.darsequran.academy.ui.manage.common.FormCheckbox
import com.darsequran.academy.ui.manage.common.FormDateField
import com.darsequran.academy.ui.manage.common.FormDropdown
import com.darsequran.academy.ui.manage.common.FormNumberField
import com.darsequran.academy.ui.manage.common.FormScreen
import com.darsequran.academy.ui.manage.common.FormTextField
import com.darsequran.academy.ui.manage.common.ManageScaffold
import com.darsequran.academy.ui.manage.common.dateOnly
import com.darsequran.academy.ui.manage.common.rememberActionRunner
import com.darsequran.academy.ui.manage.common.rememberLoader
import com.darsequran.academy.ui.manage.common.rememberPaged
import com.darsequran.academy.ui.theme.Brand

private val couponTabs = listOf(
    FilterOption(null, "Waiver Requests"),
    FilterOption("special", "Special Coupons"),
    FilterOption("default", "Default Coupons"),
)

private val applyToOptions = listOf("BOTH" to "Both Enrollment & Course Fees", "ENROLLMENT" to "Enrollment Fee Only", "COURSE" to "Course Fee Only")
private val genderOptions = listOf("" to "All Genders", "MALE" to "Male", "FEMALE" to "Female")

/** /admin/coupons: fee-waiver requests (approve, reject, delete) and the special and default coupons. */
@Composable
fun CouponsScreen(onBack: () -> Unit, onEdit: (CouponFormRoute) -> Unit, onFinance: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf<String?>(null) }
    val requests = rememberPaged("money-waiver-requests") { page, _, status -> service<MoneyApi>().waiverRequests(page, status) }
    val coupons = rememberPaged("money-coupons") { page, _, type ->
        val r = service<MoneyApi>().coupons(if (type == "special") "SPECIAL" else "DEFAULT", page)
        Paged(r.data, r.totalCount, r.page, r.pageSize)
    }
    ReloadOnReturn { requests.refresh(); coupons.refresh() }
    LaunchedEffect(tab) { if (tab != null && coupons.state.value.filter != tab) coupons.filter(tab) }
    val runner = rememberActionRunner()
    val requestState by requests.state.collectAsStateWithLifecycle()

    ManageScaffold(
        "Fee waivers & coupons", onBack, runner.snackbar,
        actions = { IconButton(onClick = onFinance) { Icon(Icons.Rounded.Insights, "Fee waiver finance") } },
        fab = if (tab == "default") ({ AddFab("New coupon", { onEdit(CouponFormRoute()) }) }) else null,
    ) { padding ->
        val header: androidx.compose.foundation.lazy.LazyListScope.() -> Unit = {
            item {
                Text("Manage default discount coupons and special student waiver requests.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                FilterChips(couponTabs, tab, onSelect = { tab = it })
            }
            if (tab == null) item {
                FilterChips(
                    listOf(FilterOption(null, "All"), FilterOption("PENDING", "Pending"), FilterOption("APPROVED", "Approved"), FilterOption("REJECTED", "Rejected")),
                    requestState.filter, requests::filter,
                )
            }
            if (tab == "special") item { Text("Coupons generated for specific students or approved fee waiver requests.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (tab == "default") item { Text("General promotional coupons available to public or specific course/gender groups.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        if (tab == null) {
            PagedListBody(requests, null, "No fee waiver requests found.", { it.id }, Modifier.padding(padding), header) { r ->
                WaiverRequestCard(r, runner) { requests.refresh(); coupons.refresh() }
            }
        } else {
            PagedListBody(coupons, null, if (tab == "special") "No special coupons created yet." else "No default coupons created yet.", { it.id }, Modifier.padding(padding), header) { c ->
                CouponCard(c, runner, onEdit = { onEdit(c.toFormRoute()) }) { coupons.refresh() }
            }
        }
    }
}

@Composable
private fun WaiverRequestCard(r: WaiverRequest, runner: ActionRunner, onChanged: () -> Unit) {
    var dialog by remember { mutableStateOf<String?>(null) }
    AppCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(r.user?.name ?: r.user?.email.orEmpty(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(r.user?.email.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!r.user?.whatsapp.isNullOrBlank()) Text("WhatsApp ${r.user?.whatsapp}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            moneyStatus(r.status).let { StatusChip(it.first, it.second) }
        }
        Spacer(Modifier.height(6.dp))
        InfoLine("Date", formatDate(r.createdAt))
        InfoLine("Course", r.courseTitle)
        InfoLine("Fee type", r.feeTypeLabel)
        Spacer(Modifier.height(4.dp))
        Text(r.reason, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (r.status == "PENDING") {
                Button(onClick = { dialog = "approve" }, enabled = !runner.busy) { Text("Approve") }
                OutlinedButton(onClick = { dialog = "reject" }, enabled = !runner.busy) { Text("Reject", color = MaterialTheme.colorScheme.error) }
            }
            if (r.status != "APPROVED") TextButton(onClick = { dialog = "delete" }, enabled = !runner.busy) { Text("Delete", color = MaterialTheme.colorScheme.error) }
        }
    }
    when (dialog) {
        "approve" -> ApproveWaiverDialog(onDismiss = { dialog = null }) { percentage, validUntil ->
            runner.run({
                service<MoneyApi>().approveRequest(r.id, formOf { text("percentage", percentage); text("validUntil", validUntil) })
            }, success = "Fee waiver request approved and coupon created.") { onChanged() }
        }
        "reject" -> ConfirmDialog(
            "Reject fee waiver request", "Are you sure you want to reject this request? The student is notified.", "Reject", destructive = true,
            onConfirm = { runner.run({ service<MoneyApi>().rejectRequest(r.id) }, success = "Fee waiver request rejected.") { onChanged() } },
            onDismiss = { dialog = null },
        )
        "delete" -> ConfirmDialog(
            "Delete this waiver request?", "The request for ${r.user?.name ?: "the student"} is removed, with its unused coupon. This cannot be undone.", "Delete", destructive = true,
            onConfirm = { runner.run({ service<MoneyApi>().deleteRequest(r.id) }, success = "Waiver request deleted.") { onChanged() } },
            onDismiss = { dialog = null },
        )
    }
}

/** "Approve Fee Waiver": the discount and how long the coupon is valid. */
@Composable
private fun ApproveWaiverDialog(onDismiss: () -> Unit, onApprove: (percentage: String, validUntil: String) -> Unit) {
    var percentage by remember { mutableStateOf("100") }
    var validUntil by remember { mutableStateOf(academyToday().plusDays(30).toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Approve fee waiver") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FormNumberField("Discount percentage (%)", percentage, { percentage = it.take(3) }, supporting = "Set to 100 for a full free course/enrollment waiver.")
                FormDateField("Valid until", validUntil, { validUntil = it }, supporting = "How long the student has to use this coupon.")
            }
        },
        confirmButton = {
            TextButton(onClick = { onDismiss(); onApprove(percentage, validUntil) }, enabled = percentage.isNotBlank() && validUntil.isNotBlank()) { Text("Approve & generate coupon") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun CouponCard(c: CouponRow, runner: ActionRunner, onEdit: () -> Unit, onChanged: () -> Unit) {
    var deleting by remember { mutableStateOf(false) }
    AppCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(c.code, style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("${c.percentage}% OFF", style = MaterialTheme.typography.titleSmall, color = Brand.success, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatusChip(c.type, if (c.type == "SPECIAL") Tone.Info else Tone.Neutral)
            StatusChip(if (c.isActive) "Active" else "Inactive", if (c.isActive) Tone.Success else Tone.Warning)
        }
        Spacer(Modifier.height(6.dp))
        InfoLine("Created", c.createdAt?.let(::formatDate) ?: "N/A")
        InfoLine("Valid", "${formatCalendarDate(c.validFrom)} – ${formatCalendarDate(c.validUntil)}")
        InfoLine("Course", c.courseTitle ?: "All Courses")
        InfoLine("Gender", c.gender?.let { com.darsequran.academy.ui.common.titleCase(it) })
        InfoLine("For", c.user?.let { "${it.name} (${it.email})" })
        InfoLine("Applies to", c.applyToLabel)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onEdit, enabled = !runner.busy) { Text("Edit") }
            TextButton(onClick = { deleting = true }, enabled = !runner.busy) { Text("Delete", color = MaterialTheme.colorScheme.error) }
        }
    }
    if (deleting) ConfirmDialog(
        "Delete ${c.code}?", "An unused coupon is deleted. A coupon that has been used is deactivated instead, to keep the payment history.", "Delete",
        destructive = true,
        onConfirm = { runner.run({ service<MoneyApi>().deleteCoupon(c.id) }) { res -> runner.message(res.stringResult("success") ?: "Coupon deleted."); onChanged() } },
        onDismiss = { deleting = false },
    )
}

private fun CouponRow.toFormRoute() = CouponFormRoute(
    id = id, type = type, code = code, percentage = percentage.toString(), validFrom = dateOnly(validFrom), validUntil = dateOnly(validUntil),
    courseId = courseId.orEmpty(), gender = gender.orEmpty(), applyTo = applyTo, isActive = isActive,
)

/** Create a DEFAULT coupon (no [CouponFormRoute.id]) or edit one; a SPECIAL coupon only changes discount, dates and active. */
@Composable
fun CouponFormScreen(route: CouponFormRoute, onBack: () -> Unit) {
    val runner = rememberActionRunner()
    val courses = rememberLoader("money-coupon-courses") { service<MoneyApi>().coupons("DEFAULT", 1).courses }
    val courseOptions = (courses.state.collectAsStateWithLifecycle().value as? Load.Ready)?.value.orEmpty().map { it.id to it.title }
    val special = route.type == "SPECIAL"
    var code by rememberSaveable { mutableStateOf(route.code) }
    var percentage by rememberSaveable { mutableStateOf(route.percentage) }
    var validFrom by rememberSaveable { mutableStateOf(route.validFrom.ifBlank { academyToday().toString() }) }
    var validUntil by rememberSaveable { mutableStateOf(route.validUntil) }
    var courseId by rememberSaveable { mutableStateOf(route.courseId) }
    var gender by rememberSaveable { mutableStateOf(route.gender) }
    var applyTo by rememberSaveable { mutableStateOf(route.applyTo) }
    var active by rememberSaveable { mutableStateOf(route.isActive) }

    FormScreen(
        if (route.id == null) "Create default coupon" else "Edit coupon", onBack, runner.busy, snackbar = runner.snackbar,
        saveLabel = if (route.id == null) "Create coupon" else "Save changes",
        onSave = {
            val parts = formOf {
                if (!special) { text("code", code.trim().uppercase()); text("courseId", courseId); text("gender", gender); text("applyTo", applyTo) }
                text("percentage", percentage); text("validFrom", validFrom); text("validUntil", validUntil)
                if (active) text("isActive", "true")
            }
            runner.run({
                if (route.id == null) service<MoneyApi>().createCoupon(parts) else service<MoneyApi>().updateCoupon(route.id, parts)
            }, success = if (route.id == null) "Coupon created." else "Coupon updated.") { onBack() }
        },
    ) {
        if (special) Text("This coupon belongs to one student's approved waiver: only the discount, validity and active state can change.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        else FormTextField("Code", code, { code = it.uppercase() }, capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Characters)
        FormNumberField("Discount percentage (%)", percentage, { percentage = it.take(3) })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FormDateField("Valid from", validFrom, { validFrom = it }, Modifier.weight(1f))
            FormDateField("Valid until", validUntil, { validUntil = it }, Modifier.weight(1f))
        }
        if (!special) {
            FormDropdown("Course (optional)", listOf("" to "All Courses") + courseOptions, courseId, { courseId = it }, placeholder = "All Courses")
            FormDropdown("Gender (optional)", genderOptions, gender, { gender = it }, placeholder = "All Genders")
            FormDropdown("Applies to fee type", applyToOptions, applyTo, { applyTo = it })
        }
        if (route.id != null) FormCheckbox("Active", active) { active = it }
    }
}

// ------------------------------------------------------ /admin/coupons/finance

private data class WaiverMeta(val range: FinanceRange, val stats: WaiverStats)

/** Claimed fee waivers: totals, filters and each waiver with its receipt. */
@Composable
fun WaiverFinanceScreen(onBack: () -> Unit, onOpenReceipt: (String) -> Unit) {
    val extras = rememberListExtras("money-waiver-finance")
    val vm = rememberPaged("money-waiver-finance") { page, q, filter ->
        val r = service<MoneyApi>().waiverFinance(page, q.ifBlank { null }, decodeFilters(filter))
        extras.value.value = WaiverMeta(r.range, r.stats)
        Paged(r.data, r.totalCount, r.page, r.pageSize)
    }
    val options = rememberLoader("money-finance-options") { service<MoneyApi>().options() }
    ReloadOnReturn(vm::refresh)
    val runner = rememberActionRunner()
    val state by vm.state.collectAsStateWithLifecycle()
    val meta = extras.value.collectAsStateWithLifecycle().value as? WaiverMeta
    val opts = (options.state.collectAsStateWithLifecycle().value as? Load.Ready)?.value

    ManageScaffold("Fee waiver finance", onBack, runner.snackbar) { padding ->
        PagedListBody(
            vm = vm,
            searchHint = null,
            emptyTitle = "No claimed fee waivers found for the selected filters.",
            key = { it.id },
            modifier = Modifier.padding(padding),
            header = {
                item { Text("Financial analytics and total claimed fee waivers across courses and students.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                item { DateRangeBar(meta?.range, state.filter, vm::filter) }
                item {
                    val s = meta?.stats ?: WaiverStats()
                    FigureGrid(listOf(
                        Figure("Total Waivers Claimed", s.totalWaiverCount.toString(), note = "Original total: ${money(s.totalOriginalFeePaise)}"),
                        Figure("Total Sum Waived Off", money(s.totalSumWaivedPaise), Tone.Success, "Net collected: ${money(s.totalNetPaidPaise)}"),
                        Figure("100% Full Waivers", s.fullWaiverCount.toString(), note = "Waived: ${money(s.fullWaiverSumPaise)}"),
                        Figure("Partial Waivers", s.partialWaiverCount.toString(), note = "Waived: ${money(s.partialWaiverSumPaise)}"),
                    ))
                }
                item { SearchField(state.query, "Student name, email, course, or label", vm::search) }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterDropdown("Course", "All courses", opts?.courses.orEmpty().map { it.id to it.title }, state.filter, "courseId", vm::filter)
                        StudentFilter(state.filter, vm::filter)
                        FilterDropdown("Payment type", "All types", opts?.incomePaymentTypes.orEmpty().map { it.value to it.label }, state.filter, "paymentType", vm::filter)
                    }
                }
            },
        ) { w ->
            AppCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(w.studentName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(w.studentEmail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    StatusChip("${w.discountPercentage}% OFF", Tone.Success)
                }
                Spacer(Modifier.height(6.dp))
                InfoLine("Date", formatDate(w.date))
                InfoLine("Course", w.courseTitle)
                InfoLine("Fee", w.paymentType.replaceFirstChar { it.uppercase() } + " fee")
                InfoLine("Original fee", money(w.originalFeePaise))
                InfoLine("Waived off", "-" + money(w.waivedAmountPaise))
                InfoLine("Net paid", money(w.netPaidPaise))
                if (w.paymentRecordId != null) {
                    Spacer(Modifier.height(6.dp))
                    ReceiptButton(w.paymentRecordId, w.receiptGeneratedAt, runner, onOpenReceipt, vm::refresh)
                }
            }
        }
    }
}
