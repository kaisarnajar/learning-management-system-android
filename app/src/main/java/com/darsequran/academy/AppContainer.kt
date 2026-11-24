package com.darsequran.academy

import android.content.Context
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
    val json = Json { ignoreUnknownKeys = true; explicitNulls = false; coerceInputValues = true; isLenient = true }
    val sessionStore = SessionStore(context, appScope, json)
    val network = Network(sessionStore, json)
    val auth = AuthRepository(network.api, sessionStore)
    val repo = AcademyRepository(network.api)
    val cart = CartStore(repo)
}
