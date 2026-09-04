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
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val totalCount: Int = 0,
    val pageSize: Int = 20,
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

    fun loadAnnouncements(page: Int = _uiState.value.currentPage, search: String = _uiState.value.searchQuery) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val searchParam = search.trim().ifEmpty { null }
            val pageSize = _uiState.value.pageSize
            when (val result = authRepository.getAnnouncements(page = page, pageSize = pageSize, search = searchParam)) {
                is NetworkResult.Success -> {
                    val apiList = result.data.data ?: emptyList()
                    // TODO: Remove fake fallback data once server API endpoints return live data
                    val list = if (apiList.isEmpty()) com.darsequran.academy.data.mock.FakeData.fakeAnnouncements else apiList
                    val totalCount = if (apiList.isEmpty()) list.size else (result.data.totalCount ?: list.size)
                    val totalPages = kotlin.math.max(1, kotlin.math.ceil(totalCount.toDouble() / pageSize.toDouble()).toInt())

                    _uiState.update { state ->
                        state.copy(
                            announcements = list,
                            filteredAnnouncements = filterAnnouncements(list, search),
                            currentPage = page,
                            totalCount = totalCount,
                            totalPages = totalPages,
                            isLoading = false
                        )
                    }
                }
                is NetworkResult.Error -> {
                    // TODO: Remove fake fallback data once server API endpoints return live data
                    val list = com.darsequran.academy.data.mock.FakeData.fakeAnnouncements
                    _uiState.update {
                        it.copy(
                            announcements = list,
                            filteredAnnouncements = filterAnnouncements(list, search),
                            totalCount = list.size,
                            totalPages = 1,
                            isLoading = false
                        )
                    }
                }
                else -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query, currentPage = 1) }
        loadAnnouncements(page = 1, search = query)
    }

    fun onPageSelected(page: Int) {
        if (page != _uiState.value.currentPage && page in 1.._uiState.value.totalPages) {
            loadAnnouncements(page = page)
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
