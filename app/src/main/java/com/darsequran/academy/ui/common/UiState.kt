package com.darsequran.academy.ui.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.darsequran.academy.AcademyApp
import com.darsequran.academy.AppContainer
import com.darsequran.academy.data.ApiResult
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

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

/** [vmFactory] for a ViewModel that keeps choices across process death in a [SavedStateHandle]. */
inline fun <reified VM : ViewModel> savedVmFactory(crossinline create: (AppContainer, SavedStateHandle) -> VM): ViewModelProvider.Factory =
    viewModelFactory { initializer { create(container, createSavedStateHandle()) } }

/**
 * Calls [reload] when the user comes back to this screen (from a screen pushed on top of it, or from another
 * app), so things like a just-submitted payment show up without a manual pull-to-refresh.
 *
 * Not after a rotation: the screen is rebuilt then, but the user never left (it used to reload every list and page on
 * each turn of the phone). Leaving is noted when the screen pauses, unless the Activity is only being recreated.
 */
@androidx.compose.runtime.Composable
fun ReloadOnReturn(reload: () -> Unit) {
    val activity = androidx.activity.compose.LocalActivity.current
    var away by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        if (away) { away = false; reload() }
        onPauseOrDispose { if (activity?.isChangingConfigurations != true) away = true }
    }
}

/**
 * Loads one thing (a record, a list) for a screen. [reload] refreshes in place (the current content stays up until
 * the new one arrives); [load] starts over from the loading state. The newest load wins, so an older answer that
 * arrives late can't replace a newer one.
 */
class LoaderViewModel<T>(private val fetch: suspend () -> ApiResult<T>) : androidx.lifecycle.ViewModel() {
    private val _state = kotlinx.coroutines.flow.MutableStateFlow<Load<T>>(Load.Loading)
    val state: kotlinx.coroutines.flow.StateFlow<Load<T>> = _state
    private var job: kotlinx.coroutines.Job? = null

    init { reload() }

    fun reload(showLoading: Boolean = false): kotlinx.coroutines.Job {
        job?.cancel()
        return viewModelScope.launch {
            if (showLoading || _state.value !is Load.Ready) _state.value = Load.Loading
            _state.value = fetch().toLoad()
        }.also { job = it }
    }

    fun load(): kotlinx.coroutines.Job = reload(showLoading = true)
}
