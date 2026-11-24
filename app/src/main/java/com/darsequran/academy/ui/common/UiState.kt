package com.darsequran.academy.ui.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.darsequran.academy.AcademyApp
import com.darsequran.academy.AppContainer
import com.darsequran.academy.data.ApiResult

/** Loading → content or error, for screens that load one thing. */
sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data class Ready<T>(val value: T) : Load<T>
    data class Failed(val message: String) : Load<Nothing>
}

fun <T> ApiResult<T>.toLoad(): Load<T> = when (this) {
    is ApiResult.Ok -> Load.Ready(value)
    is ApiResult.Err -> Load.Failed(message)
}

val CreationExtras.container: AppContainer
    get() = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AcademyApp).container

/** `viewModel(factory = vmFactory { MyViewModel(it.repo) })`. */
inline fun <reified VM : ViewModel> vmFactory(crossinline create: (AppContainer) -> VM): ViewModelProvider.Factory =
    viewModelFactory { initializer { create(container) } }

/**
 * Calls [reload] when the user comes back to this screen (from a screen pushed on top of it, or from another
 * app), so things like a just-submitted payment show up without a manual pull-to-refresh.
 */
@androidx.compose.runtime.Composable
fun ReloadOnReturn(reload: () -> Unit) {
    var seen by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        if (seen) reload() else seen = true
        onPauseOrDispose { }
    }
}
