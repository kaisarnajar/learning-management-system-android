package com.darsequran.academy

import android.content.Context
import com.darsequran.academy.data.AppEnvironment
import com.darsequran.academy.data.EnvironmentStore
import com.darsequran.academy.data.Network
import com.darsequran.academy.data.SessionStore
import com.darsequran.academy.data.repo.AcademyRepository
import com.darsequran.academy.data.repo.AuthRepository
import com.darsequran.academy.data.repo.CartStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json

/** Manual dependency container: one instance per process, created by [AcademyApp]. */
class AppContainer(context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    // encodeDefaults sends fields like acceptPolicies = true; explicitNulls = false still leaves nulls out.
    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = false; coerceInputValues = true; isLenient = true }
    // Created first: it decides which environment (and so which saved session) the app starts on.
    val environments = EnvironmentStore(context)
    val sessionStore = SessionStore(context, appScope, json)
    val network = Network(sessionStore, json)
    val auth = AuthRepository(network.api, sessionStore)
    val repo = AcademyRepository(network.api)
    val cart = CartStore(repo)

    /** A Retrofit service for one part of the API, e.g. `container.service<StudentsAdminApi>()`. */
    inline fun <reified T> service(): T = network.create(T::class.java)

    /** Points the app at another copy of the website; that environment's own session (if any) becomes current. */
    suspend fun switchEnvironment(env: AppEnvironment) {
        environments.set(env)
        sessionStore.useEnvironment(env)
        cart.clearLocal()
        repo.clearCaches()
    }
}
