package com.darsequran.academy.ui.announcements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.darsequran.academy.data.model.AnnouncementDto
import com.darsequran.academy.data.repository.AuthRepository
import com.darsequran.academy.data.repository.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AnnouncementsUiState(
    val announcements: List<AnnouncementDto> = emptyList(),
    val filteredAnnouncements: List<AnnouncementDto> = emptyList(),
    val searchQuery: String = "",
    val selectedAnnouncementDetail: AnnouncementDto? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class AnnouncementsViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnnouncementsUiState())
    val uiState: StateFlow<AnnouncementsUiState> = _uiState.asStateFlow()

    init {
        loadAnnouncements()
    }

    fun loadAnnouncements() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.getAnnouncements()) {
                is NetworkResult.Success -> {
                    val list = result.data.data ?: emptyList()
                    _uiState.update { state ->
                        state.copy(
                            announcements = list,
                            filteredAnnouncements = filterAnnouncements(list, state.searchQuery),
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
                filteredAnnouncements = filterAnnouncements(state.announcements, query)
            )
        }
    }

    fun selectAnnouncementDetail(announcement: AnnouncementDto?) {
        _uiState.update { it.copy(selectedAnnouncementDetail = announcement) }
    }

    private fun filterAnnouncements(list: List<AnnouncementDto>, query: String): List<AnnouncementDto> {
        return list.filter { item ->
            query.isBlank() ||
                    item.title.contains(query, ignoreCase = true) ||
                    (item.body?.contains(query, ignoreCase = true) == true) ||
                    (item.tag?.contains(query, ignoreCase = true) == true)
        }
    }

    class Factory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AnnouncementsViewModel(authRepository) as T
        }
    }
}
