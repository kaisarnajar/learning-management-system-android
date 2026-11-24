package com.darsequran.academy.data.repo

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.darsequran.academy.AcademyApp
import com.darsequran.academy.data.CurrentEnvironment
import com.darsequran.academy.data.remote.SessionUser
import com.darsequran.academy.data.remote.TokenResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The cart follows the session with no bookstore screen open. */
@RunWith(AndroidJUnit4::class)
class CartSessionTest {
    private val app = ApplicationProvider.getApplicationContext<AcademyApp>()
    private val server = MockWebServer()

    @Before fun setUp() {
        server.enqueue(MockResponse(body = """{"items":[{"bookId":"b1","title":"Riyad as-Salihin","quantity":2}]}"""))
        server.start()
        CurrentEnvironment.testBaseUrl = server.url("/").toString()
    }

    @After fun tearDown() { runBlocking { app.container.sessionStore.clear() }; CurrentEnvironment.testBaseUrl = null; server.close() }

    private fun awaitQuantity(q: Int) = runBlocking { withTimeout(5_000) { while (app.container.cart.quantityOf("b1") != q) delay(20) } }

    @Test
    fun signingInFetchesTheCartAndSigningOutEmptiesIt() {
        runBlocking { app.container.sessionStore.save(TokenResponse("a", "r", user = SessionUser("u1", "student@example.test"))) }
        awaitQuantity(2)
        runBlocking { app.container.sessionStore.clear() }
        awaitQuantity(0)
        assertEquals(emptyList<Any>(), app.container.cart.items.value)
    }
}
