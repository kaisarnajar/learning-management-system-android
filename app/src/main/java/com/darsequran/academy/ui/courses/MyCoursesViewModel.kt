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
    }

    fun loadCourseData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // 1. Fetch Enrollments
            when (val result = authRepository.getEnrollments()) {
                is NetworkResult.Success -> {
                    val enrollments = result.data.data ?: emptyList()
                    _uiState.update { it.copy(enrollments = enrollments, isLoading = false) }
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

    class Factory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MyCoursesViewModel(authRepository) as T
        }
    }
}
