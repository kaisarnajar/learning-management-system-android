package com.darsequran.academy.ui.courses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.darsequran.academy.data.model.AttendanceSummaryDto
import com.darsequran.academy.data.model.EnrollmentDto
import com.darsequran.academy.data.model.GradeSummaryDto
import com.darsequran.academy.data.repository.AuthRepository
import com.darsequran.academy.data.repository.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MyCoursesUiState(
    val enrollments: List<EnrollmentDto> = emptyList(),
    val userName: String? = null,
    val attendanceSummaries: Map<String, AttendanceSummaryDto> = emptyMap(), // keyed by courseId
    val gradeSummaries: Map<String, GradeSummaryDto> = emptyMap(), // keyed by courseId
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class MyCoursesViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyCoursesUiState())
    val uiState: StateFlow<MyCoursesUiState> = _uiState.asStateFlow()

    init {
        loadCourseData()
        observeUserName()
    }

    private fun observeUserName() {
        viewModelScope.launch {
            authRepository.userNameFlow.collect { name ->
                if (!name.isNullOrBlank()) {
                    _uiState.update { it.copy(userName = name) }
                }
            }
        }
    }

    fun loadCourseData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // 0. Fetch Profile for User Name
            when (val profResult = authRepository.getProfile()) {
                is NetworkResult.Success -> {
                    val name = profResult.data.user?.name
                    if (!name.isNullOrBlank()) {
                        _uiState.update { it.copy(userName = name) }
                    }
                }
                else -> {}
            }

            // 1. Fetch Enrollments
            when (val result = authRepository.getEnrollments()) {
                is NetworkResult.Success -> {
                    val rawEnrollments = result.data.data ?: emptyList()
                    val enrichedEnrollments = rawEnrollments.map { enrollment ->
                        if (enrollment.course != null && enrollment.course.description != null && enrollment.course.duration != null) {
                            enrollment
                        } else {
                            // Fetch full course details if metadata is incomplete
                            val detailsResult = authRepository.getCourseDetails(enrollment.courseId)
                            if (detailsResult is NetworkResult.Success && detailsResult.data.course != null) {
                                enrollment.copy(course = detailsResult.data.course)
                            } else {
                                enrollment
                            }
                        }
                    }
                    _uiState.update { it.copy(enrollments = enrichedEnrollments, isLoading = false) }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
                else -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }

            // 2. Fetch Attendance
            when (val attResult = authRepository.getStudentAttendance()) {
                is NetworkResult.Success -> {
                    val attMap = attResult.data.attendance?.associateBy { it.courseId } ?: emptyMap()
                    _uiState.update { it.copy(attendanceSummaries = attMap) }
                }
                else -> {}
            }

            // 3. Fetch Grades
            when (val gradeResult = authRepository.getStudentGrades()) {
                is NetworkResult.Success -> {
                    val gradeMap = gradeResult.data.grades?.associateBy { it.courseId } ?: emptyMap()
                    _uiState.update { it.copy(gradeSummaries = gradeMap) }
                }
                else -> {}
            }
        }
    }

    fun refreshData() {
        loadCourseData()
    }

    class Factory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MyCoursesViewModel(authRepository) as T
        }
    }
}
