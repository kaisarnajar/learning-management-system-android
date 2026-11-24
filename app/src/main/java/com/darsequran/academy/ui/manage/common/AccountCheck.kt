package com.darsequran.academy.ui.manage.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.darsequran.academy.AppContainer
import com.darsequran.academy.data.ApiResult
import com.darsequran.academy.data.apiCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** What the website said about an e-mail: the account's [name], or why it can't be used ([error]). */
data class AccountAnswer(val name: String?, val error: String? = null)

/**
 * "Is this e-mail a registered account?" for staff forms (add a teacher, enrol a student, record a payment).
 *
 * Changing the e-mail drops an answer given for the previous one, and [checking] always ends, even when the check is
 * cancelled by typing on (it used to stay on "Checking account…").
 */
@Stable
class AccountCheck internal constructor(
    emailState: MutableState<String>,
    nameState: MutableState<String>,
    checkedState: MutableState<String>,
    errorState: MutableState<String?>,
    private val scope: CoroutineScope,
    private val ask: suspend (String) -> ApiResult<AccountAnswer>,
) {
    var email by emailState
        private set
    var name by nameState
        private set
    private var checkedEmail by checkedState
    var error by errorState
        private set
    var checking by mutableStateOf(false)
        private set
    private var job: Job? = null

    /** The e-mail as typed has been checked and belongs to an account that can be used. */
    val verified: Boolean get() = name.isNotBlank() && error == null && checkedEmail.equals(email.trim(), ignoreCase = true)

    fun edit(value: String) {
        email = value
        if (!value.trim().equals(checkedEmail, ignoreCase = true)) { name = ""; error = null }
    }

    /** Checks the current e-mail; [onFound] gets the account's name. */
    fun verify(onFound: (String) -> Unit = {}) {
        val target = email.trim()
        job?.cancel()
        if (target.isEmpty()) { name = ""; error = null; return }
        checking = true
        job = scope.launch {
            try {
                when (val r = ask(target)) {
                    is ApiResult.Ok -> if (r.value.name != null && r.value.error == null) {
                        name = r.value.name; error = null; checkedEmail = target; onFound(r.value.name)
                    } else { name = ""; error = r.value.error ?: "This e-mail cannot be used." }
                    is ApiResult.Err -> { name = ""; error = r.message }
                }
            } finally {
                checking = false
            }
        }
    }

    fun reset() { job?.cancel(); email = ""; name = ""; checkedEmail = ""; error = null }

    /** Shows an account already linked (editing a saved record) as checked. */
    fun restore(email: String, name: String) { this.email = email; this.name = name; checkedEmail = email; error = null }
}

/** An [AccountCheck] whose answers come from [lookup] (one of the website's account lookups). */
@Composable
fun rememberAccountCheck(lookup: suspend AppContainer.(email: String) -> AccountAnswer): AccountCheck {
    val container = rememberContainer()
    val scope = rememberCoroutineScope()
    val currentLookup by rememberUpdatedState(lookup)
    val email = rememberSaveable { mutableStateOf("") }
    val name = rememberSaveable { mutableStateOf("") }
    val checked = rememberSaveable { mutableStateOf("") }
    val error = rememberSaveable { mutableStateOf<String?>(null) }
    return remember { AccountCheck(email, name, checked, error, scope) { e -> apiCall { container.currentLookup(e) } } }
}
