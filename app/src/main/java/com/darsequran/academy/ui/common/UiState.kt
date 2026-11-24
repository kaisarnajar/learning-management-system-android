package com.darsequran.academy.ui.common

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
