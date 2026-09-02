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
    val searchQuery: String = "",
    val selectedCategory: String = "All",
    val selectedPostDetail: BlogPostDto? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class BlogViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BlogUiState())
    val uiState: StateFlow<BlogUiState> = _uiState.asStateFlow()

    val categories = listOf("All", "Tajweed Tips", "Spiritual Growth", "Arabic Language", "Reflections")

    init {
        loadPosts()
    }

    fun loadPosts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.getBlogPosts()) {
                is NetworkResult.Success -> {
                    val posts = result.data.data ?: emptyList()
                    _uiState.update { state ->
                        state.copy(
                            posts = posts,
                            filteredPosts = filterPosts(posts, state.searchQuery, state.selectedCategory),
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
                filteredPosts = filterPosts(state.posts, query, state.selectedCategory)
            )
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
