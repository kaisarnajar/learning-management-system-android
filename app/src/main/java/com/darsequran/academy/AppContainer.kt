package com.darsequran.academy

import android.content.Context
import android.os.SystemClock
import com.darsequran.academy.data.AppEnvironment
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
    // Lenient parsing (unquoted strings and keys) only in release: a debug build fails on such an answer, so
    // drift in the website's API shows up while developing instead of being quietly accepted.
    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = false; coerceInputValues = true; isLenient = !BuildConfig.DEBUG }
    val sessionStore = SessionStore(context, appScope, json, initialEnvironment = AppEnvironment.current)
    val network = Network(sessionStore, json)
    val auth = AuthRepository(network.api, sessionStore)
    val repo = AcademyRepository(network.api)
    val cart = CartStore(repo)

    init {
        // A different user (sign-out, another account, another environment) must not find the last one's documents.
        val appContext = context.applicationContext
        appScope.launch {
            sessionStore.session.map { it?.user?.id }.distinctUntilChanged().drop(1).collect {
                withContext(Dispatchers.IO) { clearDocumentCache(appContext) }
            }
        }
        // The cart belongs to the signed-in student: fetch it when they sign in, empty it when they leave, whichever
        // screen is open.
        appScope.launch {
            sessionStore.session.map { it?.user?.id }
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

    private companion object {
        const val USER_CHECK_INTERVAL_MS = 10 * 60_000L
    }
}
