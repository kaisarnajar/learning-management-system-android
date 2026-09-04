package com.darsequran.academy.ui.teachers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.darsequran.academy.data.model.TeacherProfileDto
import com.darsequran.academy.data.repository.AuthRepository
import com.darsequran.academy.data.repository.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TeachersUiState(
    val teachers: List<TeacherProfileDto> = emptyList(),
    val filteredTeachers: List<TeacherProfileDto> = emptyList(),
    val specializations: List<String> = listOf("All"),
    val selectedSpecialization: String = "All",
    val searchQuery: String = "",
    val selectedTeacherDetail: TeacherProfileDto? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class TeachersViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeachersUiState())
    val uiState: StateFlow<TeachersUiState> = _uiState.asStateFlow()

    init {
        loadTeachers()
    }

    fun loadTeachers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.getTeachers()) {
                is NetworkResult.Success -> {
                    val list = result.data.data ?: emptyList()
                    val apiSpecs = list.mapNotNull { it.specialization?.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()
                    val dynamicSpecs = listOf("All") + apiSpecs

                    _uiState.update { state ->
                        state.copy(
                            teachers = list,
                            specializations = dynamicSpecs,
                            filteredTeachers = filterTeachers(list, state.searchQuery, state.selectedSpecialization),
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
                filteredTeachers = filterTeachers(state.teachers, query, state.selectedSpecialization)
            )
        }
    }

    fun onSpecializationSelected(spec: String) {
        _uiState.update { state ->
            state.copy(
                selectedSpecialization = spec,
                filteredTeachers = filterTeachers(state.teachers, state.searchQuery, spec)
            )
        }
    }

    fun selectTeacherDetail(teacher: TeacherProfileDto?) {
        _uiState.update { it.copy(selectedTeacherDetail = teacher) }
    }

    private fun filterTeachers(list: List<TeacherProfileDto>, query: String, spec: String): List<TeacherProfileDto> {
        return list.filter { teacher ->
            val matchesSearch = query.isBlank() ||
                    teacher.name.contains(query, ignoreCase = true) ||
                    (teacher.specialization?.contains(query, ignoreCase = true) == true)
            val matchesSpec = spec == "All" ||
                    (teacher.specialization?.equals(spec, ignoreCase = true) == true)
            matchesSearch && matchesSpec
        }
    }

    class Factory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TeachersViewModel(authRepository) as T
        }
    }
}
