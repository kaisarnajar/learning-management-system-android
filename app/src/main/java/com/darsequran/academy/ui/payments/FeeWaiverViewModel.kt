package com.darsequran.academy.ui.payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.darsequran.academy.data.model.CouponRequestDto
import com.darsequran.academy.data.repository.AuthRepository
import com.darsequran.academy.data.repository.NetworkResult
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FeeWaiverUiState(
    val requests: List<CouponRequestDto> = emptyList(),
    val availableCourses: List<CourseOption> = emptyList(),
    val courseTitlesMap: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showRequestModal: Boolean = false
)

class FeeWaiverViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeeWaiverUiState())
    val uiState: StateFlow<FeeWaiverUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val requestsDeferred = async { authRepository.getWaiverRequests() }
            val coursesDeferred = async { authRepository.getCourses() }
            val enrollmentsDeferred = async { authRepository.getEnrollments() }

            val requestsRes = requestsDeferred.await()
            val coursesRes = coursesDeferred.await()
            val enrollmentsRes = enrollmentsDeferred.await()

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

            if (courseOptionsList.isEmpty() && coursesRes is NetworkResult.Success) {
                coursesRes.data.data?.forEach { course ->
                    courseOptionsList.add(CourseOption(course.id, course.title))
                }
            }

            var requests = emptyList<CouponRequestDto>()
            var errorMsg: String? = null

            if (requestsRes is NetworkResult.Success) {
                requests = requestsRes.data.data ?: emptyList()
            } else if (requestsRes is NetworkResult.Error) {
                errorMsg = requestsRes.message
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    requests = requests,
                    courseTitlesMap = titlesMap,
                    availableCourses = courseOptionsList,
                    errorMessage = errorMsg
                )
            }
        }
    }

    fun openRequestModal() {
        _uiState.update { it.copy(showRequestModal = true, errorMessage = null, successMessage = null) }
    }

    fun closeRequestModal() {
        _uiState.update { it.copy(showRequestModal = false) }
    }

    fun submitWaiverRequest(
        courseId: String,
        feeType: String,
        reasonCategory: String,
        customReason: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }

            val feeTypeLabel = if (feeType == "enrollment") "Enrollment Fee" else "Course Fee"
            var finalReason = "[Fee Type: $feeTypeLabel] Reason: $reasonCategory"
            if (customReason.isNotBlank()) {
                finalReason += " - ${customReason.trim()}"
            }

            when (val result = authRepository.submitWaiverRequest(courseId, finalReason)) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            showRequestModal = false,
                            successMessage = result.data.message ?: "Your fee waiver request has been submitted successfully!"
                        )
                    }
                    loadData()
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
            return FeeWaiverViewModel(authRepository) as T
        }
    }
}
