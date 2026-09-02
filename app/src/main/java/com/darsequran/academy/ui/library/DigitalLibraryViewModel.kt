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
    val searchQuery: String = "",
    val selectedTopic: String = "All",
    val selectedBookDetail: LibraryBookDto? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class DigitalLibraryViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DigitalLibraryUiState())
    val uiState: StateFlow<DigitalLibraryUiState> = _uiState.asStateFlow()

    val topics = listOf("All", "Quran", "Tajweed", "Hadith", "Seerah", "Arabic", "Fiqh")

    init {
        loadBooks()
    }

    fun loadBooks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.getLibraryBooks()) {
                is NetworkResult.Success -> {
                    val books = result.data.data ?: emptyList()
                    _uiState.update { state ->
                        state.copy(
                            books = books,
                            filteredBooks = filterBooksList(books, state.searchQuery, state.selectedTopic),
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
                filteredBooks = filterBooksList(state.books, query, state.selectedTopic)
            )
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
