package com.darsequran.academy.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.annotation.VisibleForTesting
import com.darsequran.academy.BuildConfig
import com.darsequran.academy.data.remote.Roles
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** A copy of the website the app can talk to. Developers switch between them; everyone else stays on the default. */
enum class AppEnvironment(val key: String, val label: String, val baseUrl: String) {
    PRODUCTION("production", "Production", "https://darsequranacademy.com"),
    STAGING("staging", "Staging", BuildConfig.STAGING_BASE_URL);

    val host: String get() = baseUrl.substringAfter("://").trimEnd('/')

    companion object {
        val available: List<AppEnvironment> = entries

        /** Debug and minified (testing) builds start on staging, so testing never touches production by default. */
        val default: AppEnvironment = defaultFor(BuildConfig.DEFAULT_ENVIRONMENT)

        @VisibleForTesting
        internal fun defaultFor(key: String): AppEnvironment = fromKey(key) ?: PRODUCTION

        /** A remembered key of an environment that no longer exists (the old "local") gives null: use the default. */
        fun fromKey(key: String?): AppEnvironment? = available.firstOrNull { it.key == key }
    }
}

/**
 * The environment requests go to right now. Read on every request (by the host-rewriting interceptor) and when
 * building image and file URLs, so it is a plain volatile value rather than a flow.
 */
object CurrentEnvironment {
    @Volatile var value: AppEnvironment = AppEnvironment.default
        internal set
    val baseUrl: String get() = (testBaseUrl ?: value.baseUrl).trimEnd('/')

    /** Tests point requests at a local fake server; always null in the app. */
    @VisibleForTesting @Volatile internal var testBaseUrl: String? = null
}

private val Context.environmentDataStore by preferencesDataStore(name = "environment")

/**
 * Whether the environment switch is offered: always in debug builds; in release builds only while a developer is
 * signed in, so a phone a developer once used doesn't keep the switch for whoever uses it next.
 */
fun canSwitchEnvironment(debugBuild: Boolean, role: String?): Boolean = debugBuild || role == Roles.DEVELOPER

/** Remembers the chosen environment. */
class EnvironmentStore(private val context: Context) {
    private val envKey = stringPreferencesKey("environment")

    private val initial = runBlocking { context.environmentDataStore.data.first() }

    private val _environment = MutableStateFlow(AppEnvironment.fromKey(initial[envKey]) ?: AppEnvironment.default)
    val environment: StateFlow<AppEnvironment> = _environment.asStateFlow()

    init { CurrentEnvironment.value = _environment.value }

    /**
     * Points requests at [env] and remembers it. The UI hears about it through [publish], called once the
     * environment's own session is loaded, so no screen of the old session ever talks to the new server.
     */
    suspend fun select(env: AppEnvironment) {
        CurrentEnvironment.value = env
        context.environmentDataStore.edit { it[envKey] = env.key }
    }

    fun publish(env: AppEnvironment) { _environment.value = env }
}
