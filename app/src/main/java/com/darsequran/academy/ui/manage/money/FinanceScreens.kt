package com.darsequran.academy.ui.manage.money

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.darsequran.academy.data.remote.Paged
import com.darsequran.academy.ui.common.AppCard
import com.darsequran.academy.ui.common.Load
import com.darsequran.academy.ui.common.PagedListBody
import com.darsequran.academy.ui.common.ReloadOnReturn
import com.darsequran.academy.ui.common.StatusChip
import com.darsequran.academy.ui.common.Tone
import com.darsequran.academy.ui.common.formatDate
import com.darsequran.academy.ui.common.SearchField
import com.darsequran.academy.ui.manage.common.FilterChips
import com.darsequran.academy.ui.manage.common.FilterOption
import com.darsequran.academy.ui.manage.common.ManageScaffold
import com.darsequran.academy.ui.manage.common.rememberActionRunner
import com.darsequran.academy.ui.manage.common.rememberLoader
import com.darsequran.academy.ui.manage.common.rememberPaged
import com.darsequran.academy.ui.theme.Brand

private data class FinanceMeta(val range: FinanceRange, val summary: FinanceSummary, val filtered: FilteredTotals)

/** "Courses Finance | Book Store" switch at the top of both finance screens. */
@Composable
private fun FinanceSectionChips(bookstore: Boolean, onSwitch: () -> Unit) {
    FilterChips(
        listOf(FilterOption(null, "Courses Finance"), FilterOption("bookstore", "Book Store")),
        if (bookstore) "bookstore" else null,
        onSelect = { v -> if ((v == "bookstore") != bookstore) onSwitch() },
    )
}

/**
 * /admin/finance: income and expenses for a date range, with summary cards, filters, the filtered total,
 * Excel export and shortcuts to record a payment or an expense.
 */
@Composable
fun FinanceScreen(onBack: () -> Unit, onBookstore: () -> Unit, onRecord: (expense: Boolean) -> Unit) {
    val extras = rememberListExtras("money-finance")
    val vm = rememberPaged("money-finance") { page, q, filter ->
        val r = service<MoneyApi>().finance(page, q.ifBlank { null }, decodeFilters(filter))
        extras.value.value = FinanceMeta(r.range, r.summary, r.filtered)
        Paged(r.data, r.totalCount, r.page, r.pageSize)
    }
    val options = rememberLoader("money-finance-options") { service<MoneyApi>().options() }
    ReloadOnReturn(vm::refresh)
    val runner = rememberActionRunner()
    val context = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()
    val meta = extras.value.collectAsStateWithLifecycle().value as? FinanceMeta
    val opts = (options.state.collectAsStateWithLifecycle().value as? Load.Ready)?.value
    val filters = decodeFilters(state.filter)
    val expenses = filters["tab"] == "expenses"

    ManageScaffold("Finance", onBack, runner.snackbar) { padding ->
        PagedListBody(
            vm = vm,
            searchHint = null,
            emptyTitle = if (expenses) "No expenses for these filters." else "No income records for these filters.",
            key = { it.kind + it.id },
            modifier = Modifier.padding(padding),
            header = {
                item {
                    Text("Track student fee income and academy expenses. Summary totals use the selected date range.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                item { DateRangeBar(meta?.range, state.filter, vm::filter) }
                item {
                    val s = meta?.summary ?: FinanceSummary()
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        FigureGrid(listOf(
                            Figure("Total income", money(s.incomeTotalPaise), Tone.Success),
                            Figure("Total expenses", money(s.expenseTotalPaise), Tone.Danger),
                            Figure("Net balance", money(s.netBalancePaise), if (s.netBalancePaise >= 0) Tone.Success else Tone.Danger),
                        ))
                        BarChart(listOf(
                            Bar("Income", s.incomeTotalPaise, Brand.success),
                            Bar("Expenses", s.expenseTotalPaise, Brand.danger),
                            Bar("Net balance", s.netBalancePaise, if (s.netBalancePaise >= 0) Brand.success else Brand.danger),
                        ))
                    }
                }
                item { FinanceSectionChips(bookstore = false, onSwitch = onBookstore) }
                item {
                    FilterChips(listOf(FilterOption(null, "Income"), FilterOption("expenses", "Expense")), if (expenses) "expenses" else null, onSelect = {
                        vm.filter(withFilters(state.filter, "tab" to it))
                    })
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            runner.openExport(context, if (expenses) "Course_Expenses.xlsx" else "Course_Income.xlsx", filters + ("type" to if (expenses) "expenses" else "income") + (if (state.query.isNotBlank()) mapOf("q" to state.query) else emptyMap()))
                        }, enabled = !runner.busy, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.Download, null); Spacer(Modifier.width(6.dp)); Text("Export to Excel") }
                        Button(onClick = { onRecord(expenses) }, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(6.dp)); Text(if (expenses) "Record expense" else "Record payment") }
                    }
                }
                item {
                    SearchField(state.query, if (expenses) "Search by description, category, or teacher" else "Search by student, course, or description", vm::search)
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!expenses) {
                            FilterDropdown("Course", "All courses", opts?.courses.orEmpty().map { it.id to it.title }, state.filter, "courseId", vm::filter)
                            FilterDropdown("Student", "All students", opts?.students.orEmpty().map { it.id to (it.name?.let { n -> "$n (${it.email})" } ?: it.email) }, state.filter, "studentId", vm::filter)
                            FilterDropdown("Payment type", "All types", opts?.incomePaymentTypes.orEmpty().map { it.value to it.label }, state.filter, "paymentType", vm::filter)
                        } else {
                            FilterDropdown("Category", "All categories", opts?.expenseCategories.orEmpty().map { it.value to it.label }, state.filter, "category", vm::filter)
                            if (filters["category"] == "teacher_salary") {
                                FilterDropdown("Teacher", "All teachers", opts?.teachers.orEmpty().map { it.id to it.name }, state.filter, "teacherId", vm::filter)
                            }
                        }
                    }
                }
                item {
                    val f = meta?.filtered ?: FilteredTotals()
                    AppCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
                        Text("FILTERED RESULTS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        Row {
                            Column(Modifier.weight(1f)) {
                                Text("Total records", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("%,d".format(f.recordCount), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                            }
                            Column(Modifier.weight(1f)) {
                                Text("Total amount", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(money(f.totalPaise), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = if (expenses) Brand.danger else Brand.success)
                            }
                        }
                    }
                }
            },
        ) { row ->
            if (row.kind == "expense") ExpenseCard(row, runner, vm::refresh)
            else IncomeCard(row, runner = null, onOpenReceipt = {}, onChanged = {}, canVoid = false)
        }
    }
}

private data class BookstoreMeta(val range: FinanceRange, val summary: BookstoreSummary)

/** /admin/finance/bookstore: Book Store summary, the book sales report and the order finance report. */
@Composable
fun BookstoreFinanceScreen(onBack: () -> Unit, onCourses: () -> Unit) {
    val extras = rememberListExtras("money-bookstore")
    val vm = rememberPaged("money-bookstore") { page, q, filter ->
        val r = service<MoneyApi>().bookstore(page, q.ifBlank { null }, decodeFilters(filter))
        extras.value.value = BookstoreMeta(r.range, r.summary)
        Paged(r.data, r.totalCount, r.page, r.pageSize)
    }
    ReloadOnReturn(vm::refresh)
    val runner = rememberActionRunner()
    val context = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()
    val meta = extras.value.collectAsStateWithLifecycle().value as? BookstoreMeta
    val filters = decodeFilters(state.filter)
    val orders = filters["view"] == "orders"

    ManageScaffold("Book Store finance", onBack, runner.snackbar) { padding ->
        PagedListBody(
            vm = vm,
            searchHint = null,
            emptyTitle = if (orders) "No orders for these filters." else "No books found.",
            key = { it.kind + it.id },
            modifier = Modifier.padding(padding),
            header = {
                item { Text("Track Book Store performance, inventory costs, and revenue.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                item { DateRangeBar(meta?.range, state.filter, vm::filter) }
                item {
                    val s = meta?.summary ?: BookstoreSummary()
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        FigureGrid(listOf(
                            Figure("Total Books Procured", s.totalBooksPurchased.toString()),
                            Figure("Total Books Sold", s.totalBooksSold.toString()),
                            Figure("Current Inventory", s.currentInventoryCount.toString()),
                            Figure("Average Order Value", money(s.averageOrderValuePaise)),
                            Figure("Total Purchase Cost", money(s.totalPurchaseCostPaise), Tone.Danger),
                            Figure("Book Sales Revenue", money(s.totalSalesRevenuePaise), Tone.Success, "Excludes shipping"),
                            Figure("Shipping Collected", money(s.totalShippingCollectedPaise), note = "Passed on to the courier"),
                            Figure("Total Profit / Loss", money(s.totalProfitLossPaise), if (s.totalProfitLossPaise >= 0) Tone.Success else Tone.Danger),
                            Figure("Pending Orders Value", money(s.pendingOrdersValuePaise), Tone.Warning),
                            Figure("Approved Orders Value", money(s.approvedOrdersValuePaise), Tone.Success),
                            Figure("Completed Orders Value", money(s.completedOrdersValuePaise), Tone.Info),
                        ))
                        BarChart(listOf(
                            Bar("Purchase cost", s.totalPurchaseCostPaise, Brand.danger),
                            Bar("Sales revenue", s.totalSalesRevenuePaise, Brand.success),
                            Bar("Profit / loss", s.totalProfitLossPaise, if (s.totalProfitLossPaise >= 0) Brand.success else Brand.danger),
                        ))
                    }
                }
                item { FinanceSectionChips(bookstore = true, onSwitch = onCourses) }
                item {
                    FilterChips(listOf(FilterOption(null, "Book Sales Report"), FilterOption("orders", "Order Finance Report")), if (orders) "orders" else null, onSelect = {
                        vm.filter(withFilters(state.filter, "view" to it))
                    })
                }
                item { SearchField(state.query, if (orders) "Search by order ID, customer name, or email" else "Search by book title or author", vm::search) }
                item {
                    OutlinedButton(onClick = {
                        val params = filters - "view" + ("type" to if (orders) "book_orders" else "book_sales") + (if (state.query.isNotBlank()) mapOf("q" to state.query) else emptyMap())
                        runner.openExport(context, if (orders) "Bookstore_Order_Finance.xlsx" else "Book_Sales_Report.xlsx", params)
                    }, enabled = !runner.busy, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.Download, null); Spacer(Modifier.width(6.dp)); Text("Export to Excel") }
                }
            },
        ) { row -> if (row.kind == "order") BookOrderFinanceCard(row) else BookSaleCard(row) }
    }
}

@Composable
private fun BookSaleCard(r: BookFinanceRow) {
    AppCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
        Text(r.title.orEmpty(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(r.author.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        InfoLine("Purchase price", money(r.purchasePriceInrPaise))
        InfoLine("Selling price", money(r.sellingPriceInrPaise))
        InfoLine("Procured", r.quantityPurchased.toString())
        InfoLine("Sold", r.quantitySold.toString())
        InfoLine("Stock", r.remainingStock.toString())
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Revenue ${money(r.revenuePaise)}", style = MaterialTheme.typography.labelLarge, color = Brand.success, modifier = Modifier.weight(1f))
            Text("Profit ${money(r.profitPaise)}", style = MaterialTheme.typography.labelLarge, color = if (r.profitPaise >= 0) Brand.success else Brand.danger)
        }
    }
}

@Composable
private fun BookOrderFinanceCard(r: BookFinanceRow) {
    AppCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text("#${r.orderNumber} · ${formatDate(r.orderDate)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(r.customerName.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                Text(r.customerEmail.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(money(r.totalAmountPaise), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(6.dp))
        moneyStatus(r.status, r.statusLabel).let { StatusChip(it.first, it.second) }
        Spacer(Modifier.height(6.dp))
        InfoLine("Address", r.deliveryAddress)
        InfoLine("PIN", r.deliveryPinCode)
        InfoLine("Phone", r.deliveryPhoneNumber)
        InfoLine("Shipping", money(r.shippingChargePaise))
        if (r.refundedAmountPaise > 0) InfoLine("Refunded", money(r.refundedAmountPaise))
        InfoLine("Profit", money(r.profitContributionPaise))
    }
}
