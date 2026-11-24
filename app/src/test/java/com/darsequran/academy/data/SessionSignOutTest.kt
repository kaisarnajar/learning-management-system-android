package com.darsequran.academy.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.darsequran.academy.AcademyApp
import com.darsequran.academy.data.remote.SessionUser
import com.darsequran.academy.data.remote.TokenResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/** The session store says when the current session ends, so a release build can leave a test server. */
@RunWith(AndroidJUnit4::class)
class SessionSignOutTest {
    private val store = ApplicationProvider.getApplicationContext<AcademyApp>().container.sessionStore

    @After fun tearDown() = runBlocking { store.clear() }

    @Test
    fun signingOutIsAnnounced() = runBlocking {
        store.save(TokenResponse("a", "r", user = SessionUser("u1", "dev@example.test", role = "DEVELOPER")))
        val heard = async { withTimeout(2_000) { store.signedOut.first() } }
        kotlinx.coroutines.yield()
        store.clear()
        assertEquals(CurrentEnvironment.value, heard.await())
    }

    @Test
    fun clearingWhenNobodyIsSignedInIsNot() = runBlocking {
        val heard = async { withTimeoutOrNull(300) { store.signedOut.first() } }
        kotlinx.coroutines.yield()
        store.clear()
        assertNull(heard.await())
    }
}
