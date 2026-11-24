package com.darsequran.academy

import android.content.Context
import android.os.SystemClock
import com.darsequran.academy.data.AppEnvironment
import com.darsequran.academy.data.EnvironmentStore
import com.darsequran.academy.data.Network
import com.darsequran.academy.data.SessionStore
import com.darsequran.academy.data.clearDocumentCache
import com.darsequran.academy.data.repo.AcademyRepository
import com.darsequran.academy.data.repo.AuthRepository
import com.darsequran.academy.data.repo.CartStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/** Manual dependency container: one instance per process, created by [AcademyApp]. */
class AppContainer(context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    // encodeDefaults sends fields like acceptPolicies = true; explicitNulls = false still leaves nulls out.
    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = false; coerceInputValues = true; isLenient = true }
    // Decides which environment (and so which saved session) the app starts on.
    val environments = EnvironmentStore(context)
    val sessionStore = SessionStore(context, appScope, json, initialEnvironment = environments.environment.value)
    val network = Network(sessionStore, json)
    val auth = AuthRepository(network.api, sessionStore)
    val repo = AcademyRepository(network.api)
    val cart = CartStore(repo)

    init {
        // A different user (sign-out, another account, another environment) must not find the last one's receipts.
        val appContext = context.applicationContext
        appScope.launch {
            sessionStore.session.map { it?.user?.id }.distinctUntilChanged().drop(1).collect {
                withContext(Dispatchers.IO) { clearDocumentCache(appContext) }
            }
        }
        // The cart belongs to the signed-in student on the current server: fetch it when they sign in, empty it when
        // they leave, whichever screen is open. Keyed on the published environment, which changes only once requests
        // go to the new server.
        appScope.launch {
            combine(environments.environment, sessionStore.session) { env, s -> s?.takeIf { it.env == env }?.user?.id?.let { env to it } }
                .distinctUntilChanged()
                .collect { owner -> if (owner == null) cart.clearLocal() else cart.refresh() }
        }
    }

    @Volatile private var lastUserCheck = 0L

    /**
     * Re-reads the signed-in user (role, name) when the app comes to the front, at most every 10 minutes: a role
     * changed on the website (an admin demoted, a student made a teacher) then reaches the app without a sign-out.
     */
    fun refreshUserIfStale(now: Long = SystemClock.elapsedRealtime()) {
        if (sessionStore.session.value == null) return
        if (lastUserCheck != 0L && now - lastUserCheck < USER_CHECK_INTERVAL_MS) return
        lastUserCheck = now
        appScope.launch { auth.profile() }
    }

    /** A Retrofit service for one part of the API, e.g. `container.service<StudentsAdminApi>()`. */
    inline fun <reified T> service(): T = network.create(T::class.java)

    /** Points the app at another copy of the website; that environment's own session (if any) becomes current. */
    suspend fun switchEnvironment(env: AppEnvironment) {
        // Session first: until requests point at the new server, its token isn't attached to the old one (and the
        // old token never reaches the new one), because a session is only sent to the environment that issued it.
        sessionStore.useEnvironment(env)
        environments.select(env)
        repo.clearCaches()
        environments.publish(env)
    }

    private companion object {
        const val USER_CHECK_INTERVAL_MS = 10 * 60_000L
    }
}
