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
    val selectedTeacherDetail: com.darsequran.academy.data.model.TeacherProfileDto? = null,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val totalCount: Int = 0,
    val pageSize: Int = 20,
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

    fun loadCourses(page: Int = _uiState.value.currentPage, search: String = _uiState.value.searchQuery) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val searchParam = search.trim().ifEmpty { null }
            val pageSize = _uiState.value.pageSize
            when (val result = authRepository.getCourses(page = page, pageSize = pageSize, search = searchParam)) {
                is NetworkResult.Success -> {
                    val courses = result.data.data ?: emptyList()
                    val totalCount = result.data.totalCount ?: courses.size
                    val totalPages = kotlin.math.max(1, kotlin.math.ceil(totalCount.toDouble() / pageSize.toDouble()).toInt())
                    val apiCategories = courses.mapNotNull { it.category?.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()
                    val dynamicCategories = listOf("All") + apiCategories

                    _uiState.update { state ->
                        state.copy(
                            courses = courses,
                            categories = if (apiCategories.isEmpty()) listOf("All", "Quran", "Tajweed", "Arabic", "Fiqh") else dynamicCategories,
                            filteredCourses = courses,
                            currentPage = page,
                            totalCount = totalCount,
                            totalPages = totalPages,
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
        _uiState.update { it.copy(searchQuery = query, currentPage = 1) }
        loadCourses(page = 1, search = query)
    }

    fun onPageSelected(page: Int) {
        if (page != _uiState.value.currentPage && page in 1.._uiState.value.totalPages) {
            loadCourses(page = page)
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
        if (course != null) {
            viewModelScope.launch {
                when (val result = authRepository.getCourseDetails(course.id)) {
                    is NetworkResult.Success -> {
                        result.data.course?.let { freshCourse ->
                            _uiState.update { state -> state.copy(selectedCourseDetail = freshCourse) }
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    fun selectTeacherDetail(teacher: com.darsequran.academy.data.model.TeacherProfileDto?) {
        _uiState.update { it.copy(selectedTeacherDetail = teacher) }
    }

    fun selectTeacherDetailByName(name: String?, specialization: String? = null) {
        if (name.isNullOrBlank()) return
        val existingCourseTeacher = _uiState.value.courses.mapNotNull { it.teacher }.firstOrNull { it.name?.equals(name, ignoreCase = true) == true }
        val teacherProfile = com.darsequran.academy.data.model.TeacherProfileDto(
            id = existingCourseTeacher?.id ?: "teacher-$name",
            name = existingCourseTeacher?.name ?: name,
            specialization = existingCourseTeacher?.specialization ?: specialization ?: "Islamic Scholar & Teacher",
            bio = existingCourseTeacher?.bio,
            initials = existingCourseTeacher?.initials
        )
        _uiState.update { it.copy(selectedTeacherDetail = teacherProfile) }
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
