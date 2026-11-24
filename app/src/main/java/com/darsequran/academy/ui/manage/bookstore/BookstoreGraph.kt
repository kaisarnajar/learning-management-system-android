package com.darsequran.academy.ui.manage.bookstore

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.darsequran.academy.ui.common.Tone
import kotlinx.serialization.Serializable

// ------------------------------------------------------------------ API (/api/v1/admin/bookstore)

fun bookStatusTone(status: String): Tone = when (status) {
    "AVAILABLE" -> Tone.Success
    "OUT_OF_STOCK" -> Tone.Danger
    "COMING_SOON" -> Tone.Warning
    else -> Tone.Neutral
}

// --------------------------------------------------------------- routes

@Serializable data object BooksAdminRoute
@Serializable data class BookAdminRoute(val id: String)
@Serializable data object BookNewRoute
@Serializable data class BookEditRoute(val id: String)
@Serializable data object BookOrdersAdminRoute
@Serializable data class BookOrderAdminRoute(val id: String)

fun NavGraphBuilder.bookstoreAdminGraph(nav: NavHostController) {
    composable<BooksAdminRoute> {
        BooksListScreen(
            onBack = nav::popBackStack,
            onOpen = { nav.navigate(BookAdminRoute(it)) },
            onAdd = { nav.navigate(BookNewRoute) },
            onOrders = { nav.navigate(BookOrdersAdminRoute) },
        )
    }
    composable<BookAdminRoute> {
        val id = it.toRoute<BookAdminRoute>().id
        BookDetailScreen(id, nav::popBackStack, onEdit = { nav.navigate(BookEditRoute(id)) })
    }
    composable<BookNewRoute> { BookFormScreen(null, nav::popBackStack) }
    composable<BookEditRoute> { BookFormScreen(it.toRoute<BookEditRoute>().id, nav::popBackStack) }
    composable<BookOrdersAdminRoute> {
        BookOrdersListScreen(
            onBack = nav::popBackStack,
            onOpen = { nav.navigate(BookOrderAdminRoute(it)) },
            onBooks = { nav.navigate(BooksAdminRoute) { launchSingleTop = true } },
        )
    }
    composable<BookOrderAdminRoute> { BookOrderDetailScreen(it.toRoute<BookOrderAdminRoute>().id, nav::popBackStack) }
}
