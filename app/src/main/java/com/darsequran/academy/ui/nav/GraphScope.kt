package com.darsequran.academy.ui.nav

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel

/** Holds one navigation graph's ViewModels, so they can be cleared together when that graph is thrown away. */
class GraphViewModels : ViewModel() {
    val store = ViewModelStore()
    override fun onCleared() = store.clear()
}

/**
 * Gives [content] (a NavHost and its screens) its own ViewModel store, named [key].
 *
 * A screen's ViewModels belong to its back-stack entry, and the entries' stores live in the store NavHost finds here.
 * With the Activity's own store, a graph replaced because the user, role or environment changed left all its
 * ViewModels alive (still collecting the session and calling the API). This store is cleared when the graph leaves
 * for good, and kept while the Activity is only being recreated (rotation), so screens keep their state.
 */
@Composable
fun GraphScope(key: String, content: @Composable () -> Unit) {
    val holder: GraphViewModels = viewModel(key = "nav-graph-$key")
    val activity = LocalActivity.current
    val owner = remember(holder) { object : ViewModelStoreOwner { override val viewModelStore = holder.store } }
    DisposableEffect(holder) {
        onDispose { if (activity?.isChangingConfigurations != true) holder.store.clear() }
    }
    CompositionLocalProvider(LocalViewModelStoreOwner provides owner) { content() }
}
