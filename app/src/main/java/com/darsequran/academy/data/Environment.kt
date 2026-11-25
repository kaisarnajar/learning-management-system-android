package com.darsequran.academy.data

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.darsequran.academy.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * A copy of the website. The release build only ever talks to production. The debug build (the developer's own,
 * never shared) starts on staging and can switch between the two in Account → Server.
 */
enum class AppEnvironment(val key: String, val label: String, val baseUrl: String) {
    PRODUCTION("production", "Production", "https://darsequranacademy.com"),
    // Empty in the release build, which has no way to reach it anyway (see [available]).
    STAGING("staging", "Staging", BuildConfig.STAGING_BASE_URL);

    val host: String get() = baseUrl.substringAfter("://").trimEnd('/')

    companion object {
        /** The servers this build may talk to. */
        val available: List<AppEnvironment> = availableFor(BuildConfig.DEBUG)

        /** Where this build starts: staging in debug, so testing never touches real data by default. */
        val default: AppEnvironment = available.last()

        @VisibleForTesting
        internal fun availableFor(debugBuild: Boolean): List<AppEnvironment> =
            if (debugBuild) listOf(PRODUCTION, STAGING) else listOf(PRODUCTION)

        /** The remembered server, if this build may use it; anything else (an old key, staging in release) is null. */
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
    val host: String get() = baseUrl.substringAfter("://")

    /** Tests point requests at a local fake server; always null in the app. */
    @VisibleForTesting @Volatile internal var testBaseUrl: String? = null
}

private val Context.environmentDataStore by preferencesDataStore(name = "environment")

/** Remembers the server chosen in the debug build. The release build always comes back as production. */
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
        require(env in AppEnvironment.available) { "${env.label} is not available in this build" }
        CurrentEnvironment.value = env
        context.environmentDataStore.edit { it[envKey] = env.key }
    }

    fun publish(env: AppEnvironment) { _environment.value = env }
}
