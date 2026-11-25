package com.darsequran.academy.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.darsequran.academy.data.remote.SessionUser
import com.darsequran.academy.data.remote.TokenResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

private val Context.sessionDataStore by preferencesDataStore(name = "session")

/**
 * A signed-in session: the token pair, the user it belongs to, and the environment (server) that issued it.
 * [expiresAt] is when the access token runs out (epoch ms), or null for a session stored before the app kept it.
 */
data class Session(
    val accessToken: String,
    val refreshToken: String,
    val user: SessionUser,
    val env: AppEnvironment,
    val expiresAt: Long? = null,
)

/** When an access token issued now with [TokenResponse.expiresIn] runs out. */
fun TokenResponse.expiresAt(now: Long): Long = now + TimeUnit.SECONDS.toMillis(expiresIn.toLong())

/** What the network layer needs from the session: read it, and record the outcome of a token refresh. */
interface SessionSource {
    val session: StateFlow<Session?>

    /** Stores the token pair that replaced [old]'s, under [old]'s environment (the app may have switched since). */
    suspend fun saveRefreshed(old: Session, tokens: TokenResponse)

    /** [old]'s refresh token was rejected: sign out of [old]'s environment, unless a newer session replaced it. */
    suspend fun expire(old: Session)
}

/**
 * Persists the session. [session] is the single source of truth for "am I signed in?".
 *
 * Each environment (production, staging) keeps its own session, so switching servers in the debug build keeps you
 * signed in on each. Production uses the original key names, so sessions from before environments existed survive.
 */
class SessionStore(
    private val context: Context,
    scope: CoroutineScope,
    private val json: Json,
    initialEnvironment: AppEnvironment,
    private val clock: () -> Long = System::currentTimeMillis,
) : SessionSource {
    @Volatile private var env: AppEnvironment = initialEnvironment

    private class Keys(private val env: AppEnvironment) {
        private fun key(name: String) = stringPreferencesKey(if (env == AppEnvironment.PRODUCTION) name else "${env.key}_$name")
        val access = key("access_token")
        val refresh = key("refresh_token")
        val user = key("user")
        val expiresAt = key("access_expires_at")
    }
    private val themeKey = stringPreferencesKey("theme")

    // Held in memory first and persisted second, so a token saved by the
    // refresh authenticator is visible to the very next request.
    private val _session = MutableStateFlow(runBlocking { readOnce(env) })
    override val session: StateFlow<Session?> = _session.asStateFlow()

    /** Loads the session of [environment]; called before requests are pointed at it. */
    suspend fun useEnvironment(environment: AppEnvironment) {
        env = environment
        _session.value = readOnce(environment)
    }

    /** "system", "light" or "dark". */
    val theme: StateFlow<String> = context.sessionDataStore.data
        .map { it[themeKey] ?: "system" }
        .stateIn(scope, SharingStarted.Eagerly, "system")

    private suspend fun readOnce(env: AppEnvironment): Session? {
        val keys = Keys(env)
        val prefs = context.sessionDataStore.data.first()
        val user = prefs[keys.user]?.let { runCatching { json.decodeFromString<SessionUser>(it) }.getOrNull() } ?: return null
        val expiresAt = prefs[keys.expiresAt]?.toLongOrNull()
        return Session(prefs[keys.access] ?: return null, prefs[keys.refresh] ?: return null, user, env, expiresAt)
    }

    /** Signs in on the current environment. */
    suspend fun save(tokens: TokenResponse, user: SessionUser? = tokens.user) {
        val env = env
        val finalUser = user ?: _session.value?.user ?: return
        write(Session(tokens.accessToken, tokens.refreshToken, finalUser, env, tokens.expiresAt(clock())))
    }

    override suspend fun saveRefreshed(old: Session, tokens: TokenResponse) {
        // Signed out, or signed in again, while the refresh was in flight: that newer state wins.
        if (stored(old.env)?.refreshToken != old.refreshToken) return
        val user = tokens.user ?: old.user
        write(Session(tokens.accessToken, tokens.refreshToken, user, old.env, tokens.expiresAt(clock())))
    }

    private suspend fun stored(env: AppEnvironment): Session? = if (env == this.env) _session.value else readOnce(env)

    // NonCancellable: the in-memory session changes first, and that change can tear down the screen (and the
    // ViewModel scope) that called this, which must not stop the disk from following.
    private suspend fun write(session: Session): Unit = withContext(NonCancellable) {
        if (session.env == env) _session.value = session
        val keys = Keys(session.env)
        context.sessionDataStore.edit {
            it[keys.access] = session.accessToken
            it[keys.refresh] = session.refreshToken
            it[keys.user] = json.encodeToString(SessionUser.serializer(), session.user)
            val expiresAt = session.expiresAt
            if (expiresAt != null) it[keys.expiresAt] = expiresAt.toString() else it.remove(keys.expiresAt)
        }
    }

    /**
     * Updates the signed-in user's details. Ignored when [user] is someone else: a profile answer that arrives after
     * a sign-out and another sign-in belongs to the previous account.
     */
    suspend fun updateUser(user: SessionUser) {
        val current = _session.value ?: return
        if (current.user.id != user.id) return
        write(current.copy(user = user))
    }

    /** Signs out of the current environment. */
    suspend fun clear() = remove(env)

    override suspend fun expire(old: Session) {
        val stored = stored(old.env)
        if (stored == null || stored.refreshToken == old.refreshToken) remove(old.env)
    }

    private suspend fun remove(env: AppEnvironment): Unit = withContext(NonCancellable) {
        if (env == this@SessionStore.env) _session.value = null
        val keys = Keys(env)
        context.sessionDataStore.edit {
            it.remove(keys.access); it.remove(keys.refresh); it.remove(keys.user); it.remove(keys.expiresAt)
        }
    }

    suspend fun setTheme(value: String) {
        context.sessionDataStore.edit { it[themeKey] = value }
    }
}
