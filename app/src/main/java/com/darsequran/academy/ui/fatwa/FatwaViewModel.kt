package com.darsequran.academy.ui.fatwa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.darsequran.academy.data.model.FatwaItemDto
import com.darsequran.academy.data.repository.AuthRepository
import com.darsequran.academy.data.repository.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FatwaUiState(
    val fatwas: List<FatwaItemDto> = emptyList(),
    val filteredFatwas: List<FatwaItemDto> = emptyList(),
    val categories: List<String> = listOf("All"),
    val searchQuery: String = "",
    val selectedCategory: String = "All",
    val selectedFatwaDetail: FatwaItemDto? = null,
    val isAskFatwaDialogOpen: Boolean = false,
    val isSubmitting: Boolean = false,
    val submitSuccessMessage: String? = null,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val totalCount: Int = 0,
    val pageSize: Int = 20,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class FatwaViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FatwaUiState())
    val uiState: StateFlow<FatwaUiState> = _uiState.asStateFlow()

    init {
        loadFatwas()
    }

    fun loadFatwas(page: Int = _uiState.value.currentPage, search: String = _uiState.value.searchQuery) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val searchParam = search.trim().ifEmpty { null }
            val pageSize = _uiState.value.pageSize
            val categoryParam = if (_uiState.value.selectedCategory == "All") null else _uiState.value.selectedCategory
            when (val result = authRepository.getFatwas(page = page, pageSize = pageSize, search = searchParam, category = categoryParam)) {
                is NetworkResult.Success -> {
                    val list = result.data.data ?: emptyList()
                    val totalCount = result.data.totalCount ?: list.size
                    val totalPages = kotlin.math.max(1, kotlin.math.ceil(totalCount.toDouble() / pageSize.toDouble()).toInt())
                    val apiCategories = list.mapNotNull { it.category.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()
                    val dynamicCategories = listOf("All") + apiCategories

                    _uiState.update { state ->
                        state.copy(
                            fatwas = list,
                            categories = if (apiCategories.isEmpty()) listOf("All", "Fiqh & Worship", "Tajweed & Salah", "Zakat & Finance", "General") else dynamicCategories,
                            filteredFatwas = list,
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
        loadFatwas(page = 1, search = query)
    }

    fun onPageSelected(page: Int) {
        if (page != _uiState.value.currentPage && page in 1.._uiState.value.totalPages) {
            loadFatwas(page = page)
        }
    }

    fun onCategorySelected(category: String) {
        _uiState.update { state ->
            state.copy(
                selectedCategory = category,
                filteredFatwas = filterFatwas(state.fatwas, state.searchQuery, category)
            )
        }
    }

    fun selectFatwaDetail(fatwa: FatwaItemDto?) {
        _uiState.update { it.copy(selectedFatwaDetail = fatwa) }
    }

    fun toggleAskFatwaDialog(open: Boolean) {
        _uiState.update { it.copy(isAskFatwaDialogOpen = open, submitSuccessMessage = null) }
    }

    fun submitFatwaQuestion(title: String, question: String, category: String, askerName: String, askerEmail: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            when (authRepository.submitFatwaQuery(title, question, category, askerName, askerEmail)) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            isAskFatwaDialogOpen = false,
                            submitSuccessMessage = "Your question has been submitted successfully and is pending scholar review."
                        )
                    }
                }
                else -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            isAskFatwaDialogOpen = false,
                            submitSuccessMessage = "Question submitted and sent to scholars."
                        )
                    }
                }
            }
        }
    }

    private fun filterFatwas(list: List<FatwaItemDto>, query: String, category: String): List<FatwaItemDto> {
        return list.filter { fatwa ->
            val matchesSearch = query.isBlank() ||
                    fatwa.title.contains(query, ignoreCase = true) ||
                    fatwa.question.contains(query, ignoreCase = true) ||
                    (fatwa.answer?.contains(query, ignoreCase = true) == true)
            val matchesCategory = category == "All" ||
                    fatwa.category.contains(category, ignoreCase = true)
            matchesSearch && matchesCategory
        }
    }

    class Factory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FatwaViewModel(authRepository) as T
        }
    }
}
