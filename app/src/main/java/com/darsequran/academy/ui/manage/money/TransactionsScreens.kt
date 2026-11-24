package com.darsequran.academy.ui.manage.money

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.darsequran.academy.data.staff.formOf
import com.darsequran.academy.ui.common.Banner
import com.darsequran.academy.ui.common.ErrorState
import com.darsequran.academy.ui.common.Load
import com.darsequran.academy.ui.common.PrimaryButton
import com.darsequran.academy.ui.common.Tone
import com.darsequran.academy.ui.manage.common.ActionRunner
import com.darsequran.academy.ui.manage.common.FilterChips
import com.darsequran.academy.ui.manage.common.FilterOption
import com.darsequran.academy.ui.manage.common.FormDateField
import com.darsequran.academy.ui.manage.common.FormDropdown
import com.darsequran.academy.ui.manage.common.FormMoneyField
import com.darsequran.academy.ui.manage.common.FormTextField
import com.darsequran.academy.ui.manage.common.ManageScaffold
import com.darsequran.academy.ui.manage.common.rememberActionRunner
import com.darsequran.academy.ui.manage.common.rememberLoader
import com.darsequran.academy.ui.manage.common.AccountAnswer
import com.darsequran.academy.ui.manage.common.rememberAccountCheck
import kotlinx.coroutines.delay

/** /admin/transactions: record a payment received outside the app, or an expense. [expense] picks the tab. */
@Composable
fun TransactionsScreen(expense: Boolean, onBack: () -> Unit) {
    var expenseTab by rememberSaveable { mutableStateOf(expense) }
    val options = rememberLoader("money-finance-options") { service<MoneyApi>().options() }
    val optionsState by options.state.collectAsStateWithLifecycle()
    val opts = (optionsState as? Load.Ready)?.value
    val runner = rememberActionRunner()

    ManageScaffold("Record transactions", onBack, runner.snackbar) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Manually log payments received from students or operational expenses.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FilterChips(listOf(FilterOption(null, "Record Payment"), FilterOption("expenses", "Record Expense")), if (expenseTab) "expenses" else null, onSelect = { expenseTab = it == "expenses" })
            when {
                optionsState is Load.Failed -> ErrorState((optionsState as Load.Failed).message, { options.reload(true) })
                opts == null -> Text("Loading…")
                expenseTab -> RecordExpenseForm(opts, runner)
                else -> RecordPaymentForm(opts, runner)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RecordPaymentForm(opts: FinanceOptions, runner: ActionRunner) {
    val account = rememberAccountCheck { e -> AccountAnswer(service<MoneyApi>().lookupStudent(e).student.name) }
    var amount by rememberSaveable { mutableStateOf("") }
    var paidAt by rememberSaveable { mutableStateOf(opts.today) }
    var type by rememberSaveable { mutableStateOf("monthly") }
    var courseId by rememberSaveable { mutableStateOf("") }

    // The website looks the account up when the e-mail field loses focus; here, shortly after typing stops.
    LaunchedEffect(account.email) {
        if (account.email.isBlank() || account.verified) return@LaunchedEffect
        delay(700)
        account.verify()
    }
    val canSubmit = account.error == null && (account.email.isBlank() || account.verified)

    FormTextField("Registered account email", account.email, account::edit, keyboardType = KeyboardType.Email, supporting = "student@example.com")
    account.error?.let { Banner(it, Tone.Warning) }
    FormTextField(
        "Name (from account)",
        when {
            account.checking -> "Checking account…"
            account.name.isNotEmpty() -> account.name
            else -> "Enter an email above to load the account name."
        },
        {}, enabled = false,
    )
    FormMoneyField("Amount (₹)", amount, { amount = it })
    FormDateField("Payment date", paidAt, { paidAt = it })
    FormDropdown("Type", opts.recordPaymentTypes.map { it.value to it.label }, type, { type = it })
    FormDropdown("Course (optional)", listOf("" to "— General / not course-specific —") + opts.courses.map { it.id to it.title }, courseId, { courseId = it }, placeholder = "— General / not course-specific —")
    PrimaryButton("Add payment", {
        runner.run({
            service<MoneyApi>().recordPayment(formOf {
                text("email", account.email.trim()); text("amountInr", amount); text("paidAt", paidAt); text("paymentType", type); text("courseId", courseId)
            })
        }, success = "Transaction recorded successfully.") {
            account.reset(); amount = ""; courseId = ""; type = "monthly"; paidAt = opts.today
        }
    }, enabled = canSubmit && account.email.isNotBlank(), loading = runner.busy)
}

@Composable
private fun RecordExpenseForm(opts: FinanceOptions, runner: ActionRunner) {
    var category by rememberSaveable { mutableStateOf(opts.expenseCategories.firstOrNull()?.value ?: "teacher_salary") }
    var note by rememberSaveable { mutableStateOf("") }
    var teacherId by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var paidAt by rememberSaveable { mutableStateOf(opts.today) }
    var description by rememberSaveable { mutableStateOf("") }

    FormDropdown("Category", opts.expenseCategories.map { it.value to it.label }, category, { category = it })
    if (category == "other") FormTextField("Specify category *", note, { note = it }, supporting = "e.g. Printing, travel", maxLength = 100)
    if (category == "teacher_salary") FormDropdown("Teacher", opts.teachers.map { it.id to it.name }, teacherId, { teacherId = it }, placeholder = "Select teacher")
    FormMoneyField("Amount (₹)", amount, { amount = it })
    FormDateField("Payment date", paidAt, { paidAt = it })
    FormTextField("Description (optional)", description, { description = it }, supporting = "e.g. March salary, domain renewal", maxLength = 500)
    PrimaryButton("Record expense", {
        runner.run({
            service<MoneyApi>().recordExpense(formOf {
                text("category", category); text("amountInr", amount); text("paidAt", paidAt); text("description", description)
                if (category == "other") text("note", note)
                if (category == "teacher_salary") text("teacherId", teacherId)
            })
        }, success = "Transaction recorded successfully.") {
            note = ""; teacherId = ""; amount = ""; description = ""; paidAt = opts.today
        }
    }, loading = runner.busy)
}
