package com.darsequran.academy.data.repo

import com.darsequran.academy.data.ApiResult
import com.darsequran.academy.data.remote.CartItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The signed-in student's server cart, shared by every bookstore screen (and the badge). */
class CartStore(private val repo: AcademyRepository) {
    private val _items = MutableStateFlow<List<CartItem>>(emptyList())
    val items: StateFlow<List<CartItem>> = _items.asStateFlow()

    suspend fun refresh(): ApiResult<List<CartItem>> = repo.cart().also { if (it is ApiResult.Ok) _items.value = it.value }

    /** Sets a line's quantity (0 removes). Returns an error message, or null on success. */
    suspend fun set(bookId: String, quantity: Int): String? = when (val r = repo.setCartLine(bookId, quantity)) {
        is ApiResult.Ok -> { _items.value = r.value; null }
        is ApiResult.Err -> r.message
    }

    fun quantityOf(bookId: String) = _items.value.firstOrNull { it.bookId == bookId }?.quantity ?: 0

    fun clearLocal() { _items.value = emptyList() }
}
