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
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PaymentTab {
    PENDING,
    HISTORY
}

data class CourseOption(
    val id: String,
    val title: String
)

data class PaymentsUiState(
    val settings: PaymentSettingsDto = PaymentSettingsDto(),
    val submissions: List<PaymentSubmissionDto> = emptyList(),
    val records: List<PaymentRecordDto> = emptyList(),
    val courseTitlesMap: Map<String, String> = emptyMap(),
    val availableCourses: List<CourseOption> = emptyList(),
    val selectedTab: PaymentTab = PaymentTab.PENDING,
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

    fun selectTab(tab: PaymentTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun loadPaymentData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val settingsDeferred = async { authRepository.getPaymentSettings() }
            val historyDeferred = async { authRepository.getPaymentHistory() }
            val coursesDeferred = async { authRepository.getCourses() }
            val enrollmentsDeferred = async { authRepository.getEnrollments() }

            val settingsRes = settingsDeferred.await()
            val historyRes = historyDeferred.await()
            val coursesRes = coursesDeferred.await()
            val enrollmentsRes = enrollmentsDeferred.await()

            var newSettings = _uiState.value.settings
            if (settingsRes is NetworkResult.Success) {
                settingsRes.data.settings?.let { newSettings = it }
            }

            // Build course title lookup
            val titlesMap = mutableMapOf<String, String>()
            val courseOptionsList = mutableListOf<CourseOption>()

            if (coursesRes is NetworkResult.Success) {
                coursesRes.data.data?.forEach { course ->
                    titlesMap[course.id] = course.title
                }
            }

            if (enrollmentsRes is NetworkResult.Success) {
                enrollmentsRes.data.data?.forEach { enrollment ->
                    val cId = enrollment.courseId
                    val cTitle = enrollment.course?.title ?: titlesMap[cId] ?: cId
                    titlesMap[cId] = cTitle
                    if (courseOptionsList.none { it.id == cId }) {
                        courseOptionsList.add(CourseOption(cId, cTitle))
                    }
                }
            }

            // Fallback to general courses list for options if user has no enrollments yet
            if (courseOptionsList.isEmpty() && coursesRes is NetworkResult.Success) {
                coursesRes.data.data?.forEach { course ->
                    courseOptionsList.add(CourseOption(course.id, course.title))
                }
            }

            var submissions = emptyList<PaymentSubmissionDto>()
            var records = emptyList<PaymentRecordDto>()
            var errorMsg: String? = null

            if (historyRes is NetworkResult.Success) {
                submissions = historyRes.data.submissions ?: emptyList()
                records = historyRes.data.records ?: emptyList()
            } else if (historyRes is NetworkResult.Error) {
                errorMsg = historyRes.message
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    settings = newSettings,
                    submissions = submissions,
                    records = records,
                    courseTitlesMap = titlesMap,
                    availableCourses = courseOptionsList,
                    errorMessage = errorMsg
                )
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
