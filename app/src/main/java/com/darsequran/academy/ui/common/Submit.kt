package com.darsequran.academy.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

/** What a submitted form says back: the message to show, and whether it worked. */
data class SubmitResult(val message: String, val ok: Boolean)

/**
 * Sends a one-off form (change password, delete account, contact): one request at a time, and the outcome kept.
 *
 * A ViewModel, so the request isn't cancelled, and its outcome isn't lost, when the phone rotates mid-request (the
 * server may already have changed the password or deleted the account). [busy] is set before the request starts, so
 * a second tap in the same frame does nothing.
 */
class SubmitViewModel : ViewModel() {
    var busy by mutableStateOf(false)
        private set
    var result by mutableStateOf<SubmitResult?>(null)
        private set

    fun submit(send: suspend () -> SubmitResult) {
        if (busy) return
        busy = true
        result = null
        viewModelScope.launch {
            try { result = send() } finally { busy = false }
        }
    }
}

@Composable
fun rememberSubmitter(key: String): SubmitViewModel = viewModel(key = key) { SubmitViewModel() }
