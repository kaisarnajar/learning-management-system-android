package com.darsequran.academy.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.darsequran.academy.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** A copy of the website the app can talk to. Developers switch between them; everyone else stays on the default. */
enum class AppEnvironment(val key: String, val label: String, val baseUrl: String) {
    PRODUCTION("production", "Production", "https://darsequranacademy.com"),
    STAGING("staging", "Staging", "https://staging-learning-management-system-teal-ten.vercel.app"),
    LOCAL("local", "Local", BuildConfig.LOCAL_BASE_URL);

    val host: String get() = baseUrl.substringAfter("://").trimEnd('/')

    companion object {
        /** Local needs cleartext HTTP to a computer on the desk, so only debug builds offer it. */
        val available: List<AppEnvironment> = entries.filter { it != LOCAL || BuildConfig.LOCAL_BASE_URL.isNotBlank() }
        val default: AppEnvironment = available.firstOrNull { it.key == BuildConfig.DEFAULT_ENVIRONMENT } ?: PRODUCTION
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
    val baseUrl: String get() = value.baseUrl.trimEnd('/')
}

private val Context.environmentDataStore by preferencesDataStore(name = "environment")

/** Remembers the chosen environment, and whether a developer has signed in on this phone (which unlocks switching). */
class EnvironmentStore(private val context: Context) {
    private val envKey = stringPreferencesKey("environment")
    private val developerKey = booleanPreferencesKey("developer_device")

    private val initial = runBlocking { context.environmentDataStore.data.first() }

    private val _environment = MutableStateFlow(AppEnvironment.fromKey(initial[envKey]) ?: AppEnvironment.default)
    val environment: StateFlow<AppEnvironment> = _environment.asStateFlow()

    private val _developerDevice = MutableStateFlow(BuildConfig.DEBUG || (initial[developerKey] ?: false))
    /** True once a developer has signed in here; debug builds always allow switching. */
    val canSwitch: StateFlow<Boolean> = _developerDevice.asStateFlow()

    init { CurrentEnvironment.value = _environment.value }

    suspend fun set(env: AppEnvironment) {
        CurrentEnvironment.value = env
        _environment.value = env
        context.environmentDataStore.edit { it[envKey] = env.key }
    }

    suspend fun markDeveloperDevice() {
        if (_developerDevice.value) return
        _developerDevice.value = true
        context.environmentDataStore.edit { it[developerKey] = true }
    }
}
