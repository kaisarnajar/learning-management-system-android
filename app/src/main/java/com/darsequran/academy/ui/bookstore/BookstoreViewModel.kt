package com.darsequran.academy.ui.bookstore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.darsequran.academy.data.model.BookstoreItemDto
import com.darsequran.academy.data.repository.AuthRepository
import com.darsequran.academy.data.repository.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BookstoreUiState(
    val books: List<BookstoreItemDto> = emptyList(),
    val filteredBooks: List<BookstoreItemDto> = emptyList(),
    val categories: List<String> = listOf("All"),
    val searchQuery: String = "",
    val selectedCategory: String = "All",
    val selectedBookDetail: BookstoreItemDto? = null,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val totalCount: Int = 0,
    val pageSize: Int = 20,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class BookstoreViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookstoreUiState())
    val uiState: StateFlow<BookstoreUiState> = _uiState.asStateFlow()

    init {
        loadBookstore()
    }

    fun loadBookstore(page: Int = _uiState.value.currentPage, search: String = _uiState.value.searchQuery) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val searchParam = search.trim().ifEmpty { null }
            val pageSize = _uiState.value.pageSize
            when (val result = authRepository.getBookstoreItems(page = page, pageSize = pageSize, search = searchParam)) {
                is NetworkResult.Success -> {
                    val list = result.data.data ?: emptyList()
                    val totalCount = result.data.totalCount ?: list.size
                    val totalPages = kotlin.math.max(1, kotlin.math.ceil(totalCount.toDouble() / pageSize.toDouble()).toInt())
                    val apiCategories = list.mapNotNull { it.category?.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()
                    val dynamicCategories = listOf("All") + apiCategories

                    _uiState.update { state ->
                        state.copy(
                            books = list,
                            categories = if (apiCategories.isEmpty()) listOf("All", "Quran Editions", "Seerah", "Arabic Literature", "Hadith Studies") else dynamicCategories,
                            filteredBooks = list,
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
        loadBookstore(page = 1, search = query)
    }

    fun onPageSelected(page: Int) {
        if (page != _uiState.value.currentPage && page in 1.._uiState.value.totalPages) {
            loadBookstore(page = page)
        }
    }

    fun onCategorySelected(category: String) {
        _uiState.update { state ->
            state.copy(
                selectedCategory = category,
                filteredBooks = filterBooks(state.books, state.searchQuery, category)
            )
        }
    }

    fun selectBookDetail(book: BookstoreItemDto?) {
        _uiState.update { it.copy(selectedBookDetail = book) }
    }

    private fun filterBooks(list: List<BookstoreItemDto>, query: String, category: String): List<BookstoreItemDto> {
        return list.filter { book ->
            val matchesSearch = query.isBlank() ||
                    book.title.contains(query, ignoreCase = true) ||
                    (book.author?.contains(query, ignoreCase = true) == true) ||
                    (book.description?.contains(query, ignoreCase = true) == true)
            val matchesCategory = category == "All" ||
                    (book.category?.equals(category, ignoreCase = true) == true)
            matchesSearch && matchesCategory
        }
    }

    class Factory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BookstoreViewModel(authRepository) as T
        }
    }
}
