package com.darsequran.academy.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.darsequran.academy.data.model.AnnouncementDto
import com.darsequran.academy.data.model.EnrollmentDto
import com.darsequran.academy.data.model.InspirationDto
import com.darsequran.academy.data.repository.AuthRepository
import com.darsequran.academy.data.repository.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val inspiration: InspirationDto? = null,
    val announcements: List<AnnouncementDto> = emptyList(),
    val enrollments: List<EnrollmentDto> = emptyList(),
    val isLoadingInspiration: Boolean = false,
    val isLoadingAnnouncements: Boolean = false,
    val isLoadingEnrollments: Boolean = false,
    val errorMessage: String? = null
)

class HomeViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        fetchDailyInspiration()
        fetchAnnouncements()
        fetchEnrollments()
    }

    fun fetchDailyInspiration() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingInspiration = true) }
            when (val result = authRepository.getDailyInspiration()) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoadingInspiration = false,
                            inspiration = result.data.inspiration
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isLoadingInspiration = false) }
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun fetchAnnouncements() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAnnouncements = true) }
            when (val result = authRepository.getAnnouncements()) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoadingAnnouncements = false,
                            announcements = result.data.data ?: emptyList()
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isLoadingAnnouncements = false) }
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun fetchEnrollments() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingEnrollments = true) }
            when (val result = authRepository.getEnrollments()) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoadingEnrollments = false,
                            enrollments = result.data.data ?: emptyList()
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isLoadingEnrollments = false) }
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    class Factory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(authRepository) as T
        }
    }
}
