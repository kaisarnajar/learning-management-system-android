package com.darsequran.academy.ui.payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.darsequran.academy.data.model.PaymentRecordDto
import com.darsequran.academy.data.model.PaymentSettingsDto
import com.darsequran.academy.data.model.PaymentSubmissionDto
import com.darsequran.academy.data.model.SubmitPaymentRequest
import com.darsequran.academy.data.repository.AuthRepository
import com.darsequran.academy.data.repository.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PaymentsUiState(
    val settings: PaymentSettingsDto = PaymentSettingsDto(),
    val submissions: List<PaymentSubmissionDto> = emptyList(),
    val records: List<PaymentRecordDto> = emptyList(),
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showSubmitDialog: Boolean = false
)

class PaymentsViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentsUiState())
    val uiState: StateFlow<PaymentsUiState> = _uiState.asStateFlow()

    init {
        loadPaymentData()
    }

    fun loadPaymentData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // Fetch Settings
            when (val setRes = authRepository.getPaymentSettings()) {
                is NetworkResult.Success -> {
                    setRes.data.settings?.let { settings ->
                        _uiState.update { it.copy(settings = settings) }
                    }
                }
                else -> {}
            }

            // Fetch History
            when (val histRes = authRepository.getPaymentHistory()) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            submissions = histRes.data.submissions ?: emptyList(),
                            records = histRes.data.records ?: emptyList()
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = histRes.message) }
                }
                else -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun openSubmitDialog() {
        _uiState.update { it.copy(showSubmitDialog = true, errorMessage = null, successMessage = null) }
    }

    fun closeSubmitDialog() {
        _uiState.update { it.copy(showSubmitDialog = false) }
    }

    fun submitPaymentProof(courseId: String, utrNumber: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }

            val request = SubmitPaymentRequest(
                courseId = courseId,
                paymentType = "monthly",
                upiTransactionId = utrNumber.trim()
            )

            when (val result = authRepository.submitPayment(request)) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            showSubmitDialog = false,
                            successMessage = result.data.message ?: "Payment proof submitted successfully! Verification pending."
                        )
                    }
                    loadPaymentData()
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = result.message
                        )
                    }
                }
                else -> {}
            }
        }
    }

    class Factory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PaymentsViewModel(authRepository) as T
        }
    }
}
