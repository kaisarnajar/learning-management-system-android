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
                    _uiState.update { state ->
                        state.copy(
                            teachers = list,
                            filteredTeachers = filterTeachers(list, state.searchQuery),
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
                filteredTeachers = filterTeachers(state.teachers, query)
            )
        }
    }

    fun selectTeacherDetail(teacher: TeacherProfileDto?) {
        _uiState.update { it.copy(selectedTeacherDetail = teacher) }
    }

    private fun filterTeachers(list: List<TeacherProfileDto>, query: String): List<TeacherProfileDto> {
        return list.filter { teacher ->
            query.isBlank() ||
                    teacher.name.contains(query, ignoreCase = true) ||
                    (teacher.specialization?.contains(query, ignoreCase = true) == true) ||
                    (teacher.bio?.contains(query, ignoreCase = true) == true)
        }
    }

    class Factory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TeachersViewModel(authRepository) as T
        }
    }
}
