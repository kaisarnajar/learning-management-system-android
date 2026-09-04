package com.darsequran.academy.ui.courses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.darsequran.academy.data.model.CourseDto
import com.darsequran.academy.data.repository.AuthRepository
import com.darsequran.academy.data.repository.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CoursesCatalogUiState(
    val courses: List<CourseDto> = emptyList(),
    val filteredCourses: List<CourseDto> = emptyList(),
    val categories: List<String> = listOf("All"),
    val searchQuery: String = "",
    val selectedCategory: String = "All",
    val selectedCourseDetail: CourseDto? = null,
    val isLoading: Boolean = false,
    val isEnrolling: Boolean = false,
    val enrollmentSuccessMessage: String? = null,
    val errorMessage: String? = null
)

class CoursesCatalogViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CoursesCatalogUiState())
    val uiState: StateFlow<CoursesCatalogUiState> = _uiState.asStateFlow()

    init {
        loadCourses()
    }

    fun loadCourses() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.getCourses()) {
                is NetworkResult.Success -> {
                    val courses = result.data.data ?: emptyList()
                    val apiCategories = courses.mapNotNull { it.category?.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()
                    val dynamicCategories = listOf("All") + apiCategories

                    _uiState.update { state ->
                        state.copy(
                            courses = courses,
                            categories = if (apiCategories.isEmpty()) listOf("All", "Quran", "Tajweed", "Arabic", "Fiqh") else dynamicCategories,
                            filteredCourses = filterCoursesList(courses, state.searchQuery, state.selectedCategory),
                            isLoading = false
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
                else -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredCourses = filterCoursesList(state.courses, query, state.selectedCategory)
            )
        }
    }

    fun onCategorySelected(category: String) {
        _uiState.update { state ->
            state.copy(
                selectedCategory = category,
                filteredCourses = filterCoursesList(state.courses, state.searchQuery, category)
            )
        }
    }

    fun selectCourseDetail(course: CourseDto?) {
        _uiState.update { it.copy(selectedCourseDetail = course) }
    }

    fun requestEnrollment(courseId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isEnrolling = true, errorMessage = null) }
            when (val result = authRepository.enrollInCourse(courseId)) {
                is NetworkResult.Success -> {
                    val body = result.data
                    val msg = if (body.alreadyEnrolled == true) {
                        "You have already requested enrollment for this course."
                    } else {
                        "Enrollment request submitted successfully! Awaiting academy approval."
                    }
                    _uiState.update {
                        it.copy(
                            isEnrolling = false,
                            enrollmentSuccessMessage = msg,
                            selectedCourseDetail = null
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isEnrolling = false,
                            errorMessage = result.message ?: "Failed to submit enrollment request."
                        )
                    }
                }
                else -> {
                    _uiState.update { it.copy(isEnrolling = false) }
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(enrollmentSuccessMessage = null, errorMessage = null) }
    }

    private fun filterCoursesList(courses: List<CourseDto>, query: String, category: String): List<CourseDto> {
        return courses.filter { course ->
            val matchesSearch = query.isBlank() ||
                    course.title.contains(query, ignoreCase = true) ||
                    (course.description?.contains(query, ignoreCase = true) == true) ||
                    (course.teacher?.name?.contains(query, ignoreCase = true) == true)
            val matchesCategory = category == "All" ||
                    (course.category?.equals(category, ignoreCase = true) == true)
            matchesSearch && matchesCategory
        }
    }

    class Factory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CoursesCatalogViewModel(authRepository) as T
        }
    }
}
