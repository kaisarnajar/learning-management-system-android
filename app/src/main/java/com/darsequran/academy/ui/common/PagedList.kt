package com.darsequran.academy.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.darsequran.academy.data.ApiResult
import com.darsequran.academy.data.remote.Paged
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PagedState<T>(
    val items: List<T> = emptyList(),
    val page: Int = 0,
    val total: Int = 0,
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val query: String = "",
    val filter: String? = null,
) {
    val canLoadMore get() = !loading && items.size < total
}

/** A searchable, paginated list backed by one `/api/v1` list endpoint. */
class PagedViewModel<T>(private val fetch: suspend (page: Int, query: String, filter: String?) -> ApiResult<Paged<T>>) : ViewModel() {
    private val _state = MutableStateFlow(PagedState<T>())
    val state: StateFlow<PagedState<T>> = _state.asStateFlow()

    init { load(reset = true) }

    fun refresh() { _state.update { it.copy(refreshing = true) }; load(reset = true) }
    fun search(query: String) { _state.update { it.copy(query = query) }; load(reset = true) }
    fun filter(value: String?) { _state.update { it.copy(filter = value) }; load(reset = true) }
    fun loadMore() { if (_state.value.canLoadMore) load(reset = false) }

    private fun load(reset: Boolean) = viewModelScope.launch {
        val page = if (reset) 1 else _state.value.page + 1
        _state.update { it.copy(loading = true, error = null) }
        when (val r = fetch(page, _state.value.query, _state.value.filter)) {
            is ApiResult.Ok -> _state.update {
                it.copy(items = if (reset) r.value.data else it.items + r.value.data, page = page, total = r.value.totalCount, loading = false, refreshing = false)
            }
            is ApiResult.Err -> _state.update { it.copy(loading = false, refreshing = false, error = r.message) }
        }
    }
}

/** Standard list screen body: pull to refresh, search, infinite scroll, empty and error states. */
@Composable
fun <T> PagedListBody(
    vm: PagedViewModel<T>,
    searchHint: String?,
    emptyTitle: String,
    key: (T) -> Any,
    modifier: Modifier = Modifier,
    header: LazyListScope.() -> Unit = {},
    itemContent: @Composable (T) -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val nearEnd by remember { derivedStateOf { (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= listState.layoutInfo.totalItemsCount - 3 } }
    LaunchedEffect(nearEnd, state.items.size) { if (nearEnd) vm.loadMore() }

    PullToRefreshBox(state.refreshing, vm::refresh, modifier.fillMaxSize()) {
        LazyColumn(state = listState, contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
            if (searchHint != null) item { com.darsequran.academy.ui.courses.SearchField(state.query, searchHint, vm::search) }
            header()
            if (state.error != null && state.items.isEmpty()) item { ErrorState(state.error!!, vm::refresh) }
            else if (!state.loading && state.items.isEmpty()) item { EmptyState(emptyTitle, if (state.query.isNotBlank()) "Try a different search." else null) }
            items(state.items, key = key) { itemContent(it) }
            if (state.loading && !state.refreshing) item { if (state.items.isEmpty()) SkeletonList(4) else LoadingBox(Modifier.fillMaxWidth().height(72.dp)) }
        }
    }
}
