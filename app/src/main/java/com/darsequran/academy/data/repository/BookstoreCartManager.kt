package com.darsequran.academy.data.repository

import com.darsequran.academy.data.model.BookstoreItemDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CartItem(
    val book: BookstoreItemDto,
    val quantity: Int = 1
) {
    val totalPriceInRupees: Double
        get() = book.priceInRupees * quantity
}

object BookstoreCartManager {
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    fun addToCart(book: BookstoreItemDto, qty: Int = 1) {
        _cartItems.update { currentList ->
            val existingIndex = currentList.indexOfFirst { it.book.id == book.id }
            if (existingIndex >= 0) {
                currentList.mapIndexed { index, item ->
                    if (index == existingIndex) {
                        item.copy(quantity = item.quantity + qty)
                    } else {
                        item
                    }
                }
            } else {
                currentList + CartItem(book = book, quantity = qty)
            }
        }
    }

    fun removeFromCart(bookId: String) {
        _cartItems.update { currentList ->
            currentList.filterNot { it.book.id == bookId }
        }
    }

    fun updateQuantity(bookId: String, newQuantity: Int) {
        if (newQuantity <= 0) {
            removeFromCart(bookId)
            return
        }
        _cartItems.update { currentList ->
            currentList.map { item ->
                if (item.book.id == bookId) {
                    item.copy(quantity = newQuantity)
                } else {
                    item
                }
            }
        }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
    }

    val totalItemsCount: Int
        get() = _cartItems.value.sumOf { it.quantity }

    val totalAmountInRupees: Double
        get() = _cartItems.value.sumOf { it.totalPriceInRupees }
}
