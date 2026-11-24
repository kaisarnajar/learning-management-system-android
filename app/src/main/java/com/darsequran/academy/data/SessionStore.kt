package com.darsequran.academy.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.darsequran.academy.data.remote.SessionUser
import com.darsequran.academy.data.remote.TokenResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

private val Context.sessionDataStore by preferencesDataStore(name = "session")

/** A signed-in session: the token pair and the user it belongs to. */
data class Session(val accessToken: String, val refreshToken: String, val user: SessionUser)

/** Persists the session. [session] is the single source of truth for "am I signed in?". */
class SessionStore(private val context: Context, scope: CoroutineScope, private val json: Json) {
    private val accessKey = stringPreferencesKey("access_token")
    private val refreshKey = stringPreferencesKey("refresh_token")
    private val userKey = stringPreferencesKey("user")
    private val themeKey = stringPreferencesKey("theme")

    // Held in memory first and persisted second, so a token saved by the
    // refresh authenticator is visible to the very next request.
    private val _session = MutableStateFlow(runBlocking { readOnce() })
    val session: StateFlow<Session?> = _session.asStateFlow()

    /** "system", "light" or "dark". */
    val theme: StateFlow<String> = context.sessionDataStore.data
        .map { it[themeKey] ?: "system" }
        .stateIn(scope, SharingStarted.Eagerly, "system")

    private suspend fun readOnce(): Session? {
        val prefs = context.sessionDataStore.data.first()
        val user = prefs[userKey]?.let { runCatching { json.decodeFromString<SessionUser>(it) }.getOrNull() } ?: return null
        return Session(prefs[accessKey] ?: return null, prefs[refreshKey] ?: return null, user)
    }

    suspend fun save(tokens: TokenResponse, user: SessionUser? = tokens.user) {
        val current = _session.value?.user
        val finalUser = user ?: current ?: return
        _session.value = Session(tokens.accessToken, tokens.refreshToken, finalUser)
        context.sessionDataStore.edit {
            it[accessKey] = tokens.accessToken
            it[refreshKey] = tokens.refreshToken
            it[userKey] = json.encodeToString(SessionUser.serializer(), finalUser)
        }
    }

    suspend fun updateUser(user: SessionUser) {
        _session.value = _session.value?.copy(user = user)
        context.sessionDataStore.edit { it[userKey] = json.encodeToString(SessionUser.serializer(), user) }
    }

    suspend fun clear() {
        _session.value = null
        context.sessionDataStore.edit {
            it.remove(accessKey); it.remove(refreshKey); it.remove(userKey)
        }
    }

    suspend fun setTheme(value: String) {
        context.sessionDataStore.edit { it[themeKey] = value }
    }
}
