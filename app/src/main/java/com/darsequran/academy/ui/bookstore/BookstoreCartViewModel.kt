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
    val isSubmittingOrder: Boolean = false,
    val submissionSuccessMessage: String? = null,
    val submissionErrorMessage: String? = null,
    val submittedOrderId: String? = null,
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
        BookstoreCartManager.syncFromBackend()
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

    fun submitBookstoreOrder(
        paymentMethod: String,
        upiTransactionId: String,
        deliveryAddress: String,
        deliveryPinCode: String,
        deliveryPhoneNumber: String,
        screenshotBytes: ByteArray? = null,
        onSuccess: () -> Unit = {}
    ) {
        val selectedItems = uiState.value.cartItems.filter { uiState.value.selectedBookIds.contains(it.book.id) }
        if (selectedItems.isEmpty()) {
            _uiState.update { it.copy(submissionErrorMessage = "Please select at least one book to checkout.") }
            return
        }

        val itemsPayload = selectedItems.map { mapOf("bookId" to it.book.id, "quantity" to it.quantity) }
        val gson = com.google.gson.Gson()
        val itemsJson = gson.toJson(itemsPayload)

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingOrder = true, submissionErrorMessage = null, submissionSuccessMessage = null) }

            val res = authRepository.submitBookstoreOrder(
                itemsJson = itemsJson,
                paymentMethod = paymentMethod,
                upiTransactionId = upiTransactionId,
                deliveryAddress = deliveryAddress,
                deliveryPinCode = deliveryPinCode,
                deliveryPhoneNumber = deliveryPhoneNumber,
                screenshotBytes = screenshotBytes
            )

            when (res) {
                is NetworkResult.Success -> {
                    if (res.data.success) {
                        selectedItems.forEach { item ->
                            BookstoreCartManager.removeFromCart(item.book.id)
                        }
                        _uiState.update { state ->
                            state.copy(
                                isSubmittingOrder = false,
                                submissionSuccessMessage = res.data.message ?: "Order submitted successfully for Admin approval!",
                                submittedOrderId = res.data.orderId,
                                selectedBookIds = emptySet()
                            )
                        }
                        loadOrders()
                        onSuccess()
                    } else {
                        _uiState.update { state ->
                            state.copy(
                                isSubmittingOrder = false,
                                submissionErrorMessage = res.data.error ?: "Order submission failed."
                            )
                        }
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update { state ->
                        state.copy(
                            isSubmittingOrder = false,
                            submissionErrorMessage = res.message
                        )
                    }
                }
                is NetworkResult.Loading -> {
                    // Handled via isSubmittingOrder
                }
            }
        }
    }

    fun clearSubmissionState() {
        _uiState.update {
            it.copy(
                isSubmittingOrder = false,
                submissionSuccessMessage = null,
                submissionErrorMessage = null
            )
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
