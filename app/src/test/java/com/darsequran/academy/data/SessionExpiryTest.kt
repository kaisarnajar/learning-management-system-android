package com.darsequran.academy.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.darsequran.academy.AcademyApp
import com.darsequran.academy.data.remote.SessionUser
import com.darsequran.academy.data.remote.TokenResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** The session remembers when its access token runs out, so the network layer can refresh it in time. */
@RunWith(AndroidJUnit4::class)
class SessionExpiryTest {
    private val app = ApplicationProvider.getApplicationContext<AcademyApp>()
    private val json = Json { ignoreUnknownKeys = true }
    private val user = SessionUser("u1", "one@example.test")
    private var now = 5_000_000L

    private fun store() =
        SessionStore(app, CoroutineScope(Dispatchers.Unconfined), json, AppEnvironment.default) { now }

    @After fun tearDown() = runBlocking { store().clear() }

    @Test
    fun signingInRecordsTheExpiryAndItSurvivesARestart() = runBlocking {
        store().save(TokenResponse("a", "r", expiresIn = 3600, user = user))
        assertEquals(now + 3_600_000L, store().session.value?.expiresAt)
    }

    @Test
    fun aRefreshMovesTheExpiry() = runBlocking {
        val store = store()
        store.save(TokenResponse("a", "r", expiresIn = 3600, user = user))
        now += 50 * 60_000L
        store.saveRefreshed(store.session.value!!, TokenResponse("a2", "r2", expiresIn = 3600))
        assertEquals(now + 3_600_000L, store.session.value?.expiresAt)
        assertEquals(now + 3_600_000L, store().session.value?.expiresAt)
    }

    @Test
    fun aProfileUpdateKeepsTheExpiry() = runBlocking {
        val store = store()
        store.save(TokenResponse("a", "r", expiresIn = 3600, user = user))
        store.updateUser(user.copy(name = "One"))
        assertEquals(now + 3_600_000L, store().session.value?.expiresAt)
    }
}
