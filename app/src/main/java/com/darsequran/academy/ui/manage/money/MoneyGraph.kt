package com.darsequran.academy.ui.manage.money

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

// --------------------------------------------------------------- routes

@Serializable data object PendingPaymentsRoute
@Serializable data object CompletedPaymentsRoute
@Serializable data object ReceiptsRoute
@Serializable data class ReceiptRoute(val recordId: String)
@Serializable data object FinanceRoute
@Serializable data object BookstoreFinanceRoute
@Serializable data class TransactionsRoute(val expense: Boolean = false)
@Serializable data object PaymentSettingsRoute
@Serializable data object ShippingChargesRoute
@Serializable data object CouponsRoute
@Serializable data object WaiverFinanceRoute

/** The coupon form; an edit carries the coupon's current values (there is no single-coupon read). */
@Serializable
data class CouponFormRoute(
    val id: String? = null,
    val type: String = "DEFAULT",
    val code: String = "",
    val percentage: String = "",
    val validFrom: String = "",
    val validUntil: String = "",
    val courseId: String = "",
    val gender: String = "",
    val applyTo: String = "BOTH",
    val isActive: Boolean = true,
)


fun NavGraphBuilder.moneyGraph(nav: NavHostController) {
    val openReceipt: (String) -> Unit = { nav.navigate(ReceiptRoute(it)) }
    composable<PendingPaymentsRoute> { PendingPaymentsScreen(nav::popBackStack, openReceipt) }
    composable<CompletedPaymentsRoute> { CompletedPaymentsScreen(nav::popBackStack, openReceipt) { nav.navigate(ReceiptsRoute) } }
    composable<ReceiptsRoute> { ReceiptsScreen(nav::popBackStack, openReceipt) }
    composable<ReceiptRoute> { ReceiptDetailScreen(it.toRoute<ReceiptRoute>().recordId, nav::popBackStack) }
    composable<FinanceRoute> {
        FinanceScreen(
            nav::popBackStack,
            onBookstore = { nav.navigate(BookstoreFinanceRoute) { popUpTo<FinanceRoute> { inclusive = true } } },
            onRecord = { expense -> nav.navigate(TransactionsRoute(expense)) },
        )
    }
    composable<BookstoreFinanceRoute> {
        BookstoreFinanceScreen(nav::popBackStack, onCourses = { nav.navigate(FinanceRoute) { popUpTo<BookstoreFinanceRoute> { inclusive = true } } })
    }
    composable<TransactionsRoute> { TransactionsScreen(it.toRoute<TransactionsRoute>().expense, nav::popBackStack) }
    composable<PaymentSettingsRoute> { PaymentSettingsScreen(nav::popBackStack) }
    composable<ShippingChargesRoute> { ShippingChargesScreen(nav::popBackStack) }
    composable<CouponsRoute> { CouponsScreen(nav::popBackStack, onEdit = { nav.navigate(it) }, onFinance = { nav.navigate(WaiverFinanceRoute) }) }
    composable<CouponFormRoute> { CouponFormScreen(it.toRoute<CouponFormRoute>(), nav::popBackStack) }
    composable<WaiverFinanceRoute> { WaiverFinanceScreen(nav::popBackStack, openReceipt) }
}
