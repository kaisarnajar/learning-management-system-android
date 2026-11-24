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

/** What the network layer needs from the session: read it, store a refreshed token pair, or drop it. */
interface SessionSource {
    val session: StateFlow<Session?>
    suspend fun save(tokens: TokenResponse, user: SessionUser? = tokens.user)
    suspend fun clear()
}

/**
 * Persists the session. [session] is the single source of truth for "am I signed in?".
 *
 * Each environment (production, staging, local) keeps its own session, so a developer who switches keeps being
 * signed in on each. Production uses the original key names, so sessions from before environments existed survive.
 */
class SessionStore(private val context: Context, scope: CoroutineScope, private val json: Json) : SessionSource {
    private var env: AppEnvironment = CurrentEnvironment.value
    private fun key(name: String) = stringPreferencesKey(if (env == AppEnvironment.PRODUCTION) name else "${env.key}_$name")
    private val accessKey get() = key("access_token")
    private val refreshKey get() = key("refresh_token")
    private val userKey get() = key("user")
    private val themeKey = stringPreferencesKey("theme")

    // Held in memory first and persisted second, so a token saved by the
    // refresh authenticator is visible to the very next request.
    private val _session = MutableStateFlow(runBlocking { readOnce() })
    override val session: StateFlow<Session?> = _session.asStateFlow()

    /** Loads the session of [environment] (after the app switched to it). */
    suspend fun useEnvironment(environment: AppEnvironment) {
        env = environment
        _session.value = readOnce()
    }

    /** "system", "light" or "dark". */
    val theme: StateFlow<String> = context.sessionDataStore.data
        .map { it[themeKey] ?: "system" }
        .stateIn(scope, SharingStarted.Eagerly, "system")

    private suspend fun readOnce(): Session? {
        val prefs = context.sessionDataStore.data.first()
        val user = prefs[userKey]?.let { runCatching { json.decodeFromString<SessionUser>(it) }.getOrNull() } ?: return null
        return Session(prefs[accessKey] ?: return null, prefs[refreshKey] ?: return null, user)
    }

    override suspend fun save(tokens: TokenResponse, user: SessionUser?) {
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

    override suspend fun clear() {
        _session.value = null
        context.sessionDataStore.edit {
            it.remove(accessKey); it.remove(refreshKey); it.remove(userKey)
        }
    }

    suspend fun setTheme(value: String) {
        context.sessionDataStore.edit { it[themeKey] = value }
    }
}
