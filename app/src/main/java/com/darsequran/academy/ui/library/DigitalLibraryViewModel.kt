package com.darsequran.academy.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.darsequran.academy.data.model.LibraryBookDto
import com.darsequran.academy.data.repository.AuthRepository
import com.darsequran.academy.data.repository.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DigitalLibraryUiState(
    val books: List<LibraryBookDto> = emptyList(),
    val filteredBooks: List<LibraryBookDto> = emptyList(),
    val topics: List<String> = listOf("All"),
    val searchQuery: String = "",
    val selectedTopic: String = "All",
    val selectedBookDetail: LibraryBookDto? = null,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val totalCount: Int = 0,
    val pageSize: Int = 20,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class DigitalLibraryViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DigitalLibraryUiState())
    val uiState: StateFlow<DigitalLibraryUiState> = _uiState.asStateFlow()

    init {
        loadBooks()
    }

    fun loadBooks(page: Int = _uiState.value.currentPage, search: String = _uiState.value.searchQuery) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val searchParam = search.trim().ifEmpty { null }
            val pageSize = _uiState.value.pageSize
            val topicParam = if (_uiState.value.selectedTopic == "All") null else _uiState.value.selectedTopic
            when (val result = authRepository.getLibraryBooks(page = page, pageSize = pageSize, search = searchParam, topic = topicParam)) {
                is NetworkResult.Success -> {
                    val books = result.data.data ?: emptyList()
                    val totalCount = result.data.totalCount ?: books.size
                    val totalPages = kotlin.math.max(1, kotlin.math.ceil(totalCount.toDouble() / pageSize.toDouble()).toInt())
                    val apiTopics = books.flatMap { listOfNotNull(it.topic, it.category) }
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()
                    val dynamicTopics = listOf("All") + apiTopics

                    _uiState.update { state ->
                        state.copy(
                            books = books,
                            topics = if (apiTopics.isEmpty()) listOf("All", "Quran", "Tajweed", "Hadith", "Seerah", "Arabic", "Fiqh") else dynamicTopics,
                            filteredBooks = books,
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
        loadBooks(page = 1, search = query)
    }

    fun onPageSelected(page: Int) {
        if (page != _uiState.value.currentPage && page in 1.._uiState.value.totalPages) {
            loadBooks(page = page)
        }
    }

    fun onTopicSelected(topic: String) {
        _uiState.update { state ->
            state.copy(
                selectedTopic = topic,
                filteredBooks = filterBooksList(state.books, state.searchQuery, topic)
            )
        }
    }

    fun selectBookDetail(book: LibraryBookDto?) {
        _uiState.update { it.copy(selectedBookDetail = book) }
    }

    private fun filterBooksList(books: List<LibraryBookDto>, query: String, topic: String): List<LibraryBookDto> {
        return books.filter { book ->
            val matchesSearch = query.isBlank() ||
                    book.title.contains(query, ignoreCase = true) ||
                    (book.author?.contains(query, ignoreCase = true) == true) ||
                    (book.description?.contains(query, ignoreCase = true) == true)
            val matchesTopic = topic == "All" ||
                    (book.topic?.contains(topic, ignoreCase = true) == true) ||
                    (book.category?.contains(topic, ignoreCase = true) == true)
            matchesSearch && matchesTopic
        }
    }

    class Factory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DigitalLibraryViewModel(authRepository) as T
        }
    }
}
