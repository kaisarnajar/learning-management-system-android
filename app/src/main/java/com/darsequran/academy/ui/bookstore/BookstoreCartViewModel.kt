package com.darsequran.academy.ui.bookstore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.darsequran.academy.data.model.BookOrderDto
import com.darsequran.academy.data.repository.AuthRepository
import com.darsequran.academy.data.repository.BookstoreCartManager
import com.darsequran.academy.data.repository.CartItem
import com.darsequran.academy.data.repository.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CartUiState(
    val cartItems: List<CartItem> = emptyList(),
    val selectedBookIds: Set<String> = emptySet(),
    val pastOrders: List<BookOrderDto> = emptyList(),
    val isLoadingOrders: Boolean = false,
    val errorMessage: String? = null
)

class BookstoreCartViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CartUiState())
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            BookstoreCartManager.cartItems.collect { items ->
                val allIds = items.map { it.book.id }.toSet()
                _uiState.update { state ->
                    val updatedSelected = if (state.selectedBookIds.isEmpty()) allIds else state.selectedBookIds.intersect(allIds)
                    state.copy(cartItems = items, selectedBookIds = updatedSelected)
                }
            }
        }
        loadOrders()
    }

    fun loadOrders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingOrders = true) }
            when (val res = authRepository.getBookstoreOrders()) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isLoadingOrders = false, pastOrders = res.data.orders ?: emptyList()) }
                }
                else -> {
                    _uiState.update { it.copy(isLoadingOrders = false) }
                }
            }
        }
    }

    fun toggleSelect(bookId: String) {
        _uiState.update { state ->
            val set = state.selectedBookIds.toMutableSet()
            if (set.contains(bookId)) set.remove(bookId) else set.add(bookId)
            state.copy(selectedBookIds = set)
        }
    }

    fun selectAll() {
        _uiState.update { state ->
            state.copy(selectedBookIds = state.cartItems.map { it.book.id }.toSet())
        }
    }

    fun deselectAll() {
        _uiState.update { state ->
            state.copy(selectedBookIds = emptySet())
        }
    }

    class Factory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BookstoreCartViewModel(authRepository) as T
        }
    }
}
