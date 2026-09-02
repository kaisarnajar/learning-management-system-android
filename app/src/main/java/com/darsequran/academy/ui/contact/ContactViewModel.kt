package com.darsequran.academy.ui.contact

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.darsequran.academy.data.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ContactUiState(
    val isSubmitting: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

class ContactViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactUiState())
    val uiState: StateFlow<ContactUiState> = _uiState.asStateFlow()

    fun submitInquiry(
        name: String,
        email: String,
        phone: String,
        subject: String,
        message: String
    ) {
        if (name.isBlank() || email.isBlank() || message.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please fill in all required fields.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, successMessage = null) }
            // Simulate network submission with delay
            delay(1200)
            _uiState.update {
                it.copy(
                    isSubmitting = false,
                    successMessage = "Thank you! Your message has been received. We aim to respond within 2–3 business days."
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null) }
    }

    class Factory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ContactViewModel(authRepository) as T
        }
    }
}
