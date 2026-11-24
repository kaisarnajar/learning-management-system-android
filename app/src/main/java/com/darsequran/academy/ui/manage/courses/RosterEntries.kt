package com.darsequran.academy.ui.manage.courses

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Each student's entry on a roster form (present/absent, marks) and whether the entries were filled from the server.
 *
 * The two must live and die together. When the entries were in `remember` and the flag in `rememberSaveable`, a
 * rotation emptied the entries while the flag said "filled", so every student showed as present (or ungraded) and
 * saving overwrote the stored records. In a ViewModel both survive a rotation; after process death both are gone
 * and the form is filled again from the server.
 */
class RosterEntries<V> : ViewModel() {
    val values = mutableStateMapOf<String, V>()
    var initialised by mutableStateOf(false)

    /** Fills the entries once, from [initial] (enrollment id → value). */
    fun initialiseOnce(initial: () -> Map<String, V>) {
        if (initialised) return
        values.putAll(initial())
        initialised = true
    }
}

/** The [RosterEntries] of one form, kept per back-stack entry under [key]. */
@Composable
fun <V> rememberRosterEntries(key: String): RosterEntries<V> = viewModel(key = key) { RosterEntries<V>() }
