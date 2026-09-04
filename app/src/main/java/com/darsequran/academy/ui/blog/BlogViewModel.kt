package com.darsequran.academy.ui.blog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.darsequran.academy.data.model.BlogPostDto
import com.darsequran.academy.data.repository.AuthRepository
import com.darsequran.academy.data.repository.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BlogUiState(
    val posts: List<BlogPostDto> = emptyList(),
    val filteredPosts: List<BlogPostDto> = emptyList(),
    val categories: List<String> = listOf("All"),
    val searchQuery: String = "",
    val selectedCategory: String = "All",
    val selectedPostDetail: BlogPostDto? = null,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val totalCount: Int = 0,
    val pageSize: Int = 20,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class BlogViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BlogUiState())
    val uiState: StateFlow<BlogUiState> = _uiState.asStateFlow()

    init {
        loadPosts()
    }

    fun loadPosts(page: Int = _uiState.value.currentPage, search: String = _uiState.value.searchQuery) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val searchParam = search.trim().ifEmpty { null }
            val pageSize = _uiState.value.pageSize
            when (val result = authRepository.getBlogPosts(page = page, pageSize = pageSize, search = searchParam)) {
                is NetworkResult.Success -> {
                    val apiPosts = result.data.data ?: emptyList()
                    // TODO: Remove fake fallback data once server API endpoints return live data
                    val posts = if (apiPosts.isEmpty()) com.darsequran.academy.data.mock.FakeData.fakeBlogPosts else apiPosts
                    val totalCount = if (apiPosts.isEmpty()) posts.size else (result.data.totalCount ?: posts.size)
                    val totalPages = kotlin.math.max(1, kotlin.math.ceil(totalCount.toDouble() / pageSize.toDouble()).toInt())
                    val apiCategories = posts.mapNotNull { it.category?.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()
                    val dynamicCategories = listOf("All") + apiCategories

                    _uiState.update { state ->
                        state.copy(
                            posts = posts,
                            categories = if (apiCategories.isEmpty()) listOf("All", "Tajweed Tips", "Spiritual Growth", "Reflections", "Adab & Ethics") else dynamicCategories,
                            filteredPosts = filterPosts(posts, search, state.selectedCategory),
                            currentPage = page,
                            totalCount = totalCount,
                            totalPages = totalPages,
                            isLoading = false
                        )
                    }
                }
                is NetworkResult.Error -> {
                    // TODO: Remove fake fallback data once server API endpoints return live data
                    val posts = com.darsequran.academy.data.mock.FakeData.fakeBlogPosts
                    val apiCategories = posts.mapNotNull { it.category?.trim() }.filter { it.isNotBlank() }.distinct()
                    _uiState.update { state ->
                        state.copy(
                            posts = posts,
                            categories = listOf("All") + apiCategories,
                            filteredPosts = filterPosts(posts, search, state.selectedCategory),
                            totalCount = posts.size,
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
        loadPosts(page = 1, search = query)
    }

    fun onPageSelected(page: Int) {
        if (page != _uiState.value.currentPage && page in 1.._uiState.value.totalPages) {
            loadPosts(page = page)
        }
    }

    fun onCategorySelected(category: String) {
        _uiState.update { state ->
            state.copy(
                selectedCategory = category,
                filteredPosts = filterPosts(state.posts, state.searchQuery, category)
            )
        }
    }

    fun selectPostDetail(post: BlogPostDto?) {
        _uiState.update { it.copy(selectedPostDetail = post) }
    }

    private fun filterPosts(posts: List<BlogPostDto>, query: String, category: String): List<BlogPostDto> {
        return posts.filter { post ->
            val matchesSearch = query.isBlank() ||
                    post.title.contains(query, ignoreCase = true) ||
                    (post.excerpt?.contains(query, ignoreCase = true) == true) ||
                    (post.body?.contains(query, ignoreCase = true) == true)
            val matchesCategory = category == "All" ||
                    (post.category?.equals(category, ignoreCase = true) == true)
            matchesSearch && matchesCategory
        }
    }

    class Factory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BlogViewModel(authRepository) as T
        }
    }
}
