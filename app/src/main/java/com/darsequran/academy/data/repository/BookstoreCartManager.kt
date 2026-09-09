package com.darsequran.academy.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.darsequran.academy.data.model.BookstoreItemDto
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CartItem(
    val book: BookstoreItemDto,
    val quantity: Int = 1
) {
    val totalPriceInRupees: Double
        get() = book.priceInRupees * quantity
}

object BookstoreCartManager {
    private const val PREFS_NAME = "dqa_bookstore_cart_prefs"
    private const val KEY_CART_ITEMS = "dqa-bookstore-cart"

    private val gson = Gson()
    private var sharedPreferences: SharedPreferences? = null
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private var repository: AuthRepository? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun init(context: Context) {
        if (sharedPreferences == null) {
            sharedPreferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            loadFromPreferences()
        }
    }

    fun attachRepository(authRepository: AuthRepository) {
        this.repository = authRepository
        syncFromBackend()
    }

    fun syncFromBackend() {
        val repo = repository ?: return
        scope.launch {
            when (val res = repo.getBookstoreCart()) {
                is NetworkResult.Success -> {
                    val serverItems = res.data.items?.map {
                        CartItem(book = it.book, quantity = it.quantity)
                    } ?: emptyList()

                    if (serverItems.isNotEmpty()) {
                        _cartItems.value = serverItems
                        saveToPreferences(serverItems)
                    } else if (_cartItems.value.isNotEmpty()) {
                        repo.syncBookstoreCart(_cartItems.value)
                    }
                }
                else -> {}
            }
        }
    }

    private fun pushToBackend(items: List<CartItem>) {
        val repo = repository ?: return
        scope.launch {
            repo.syncBookstoreCart(items)
        }
    }

    private fun loadFromPreferences() {
        val prefs = sharedPreferences ?: return
        val json = prefs.getString(KEY_CART_ITEMS, null) ?: return
        try {
            val type = object : TypeToken<List<CartItem>>() {}.type
            val savedItems: List<CartItem>? = gson.fromJson(json, type)
            if (!savedItems.isNullOrEmpty()) {
                _cartItems.value = savedItems
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveToPreferences(items: List<CartItem>) {
        val prefs = sharedPreferences ?: return
        try {
            val json = gson.toJson(items)
            prefs.edit().putString(KEY_CART_ITEMS, json).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setServerCartItems(items: List<CartItem>) {
        _cartItems.value = items
        saveToPreferences(items)
        pushToBackend(items)
    }

    fun addToCart(book: BookstoreItemDto, qty: Int = 1) {
        _cartItems.update { currentList ->
            val existingIndex = currentList.indexOfFirst { it.book.id == book.id }
            val newList = if (existingIndex >= 0) {
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
            saveToPreferences(newList)
            pushToBackend(newList)
            newList
        }
    }

    fun removeFromCart(bookId: String) {
        _cartItems.update { currentList ->
            val newList = currentList.filterNot { it.book.id == bookId }
            saveToPreferences(newList)
            pushToBackend(newList)
            newList
        }
    }

    fun updateQuantity(bookId: String, newQuantity: Int) {
        if (newQuantity <= 0) {
            removeFromCart(bookId)
            return
        }
        _cartItems.update { currentList ->
            val newList = currentList.map { item ->
                if (item.book.id == bookId) {
                    item.copy(quantity = newQuantity)
                } else {
                    item
                }
            }
            saveToPreferences(newList)
            pushToBackend(newList)
            newList
        }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        saveToPreferences(emptyList())
        pushToBackend(emptyList())
    }

    val totalItemsCount: Int
        get() = _cartItems.value.sumOf { it.quantity }

    val totalAmountInRupees: Double
        get() = _cartItems.value.sumOf { it.totalPriceInRupees }
}
