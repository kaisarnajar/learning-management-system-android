package com.darsequran.academy.data.repo

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.darsequran.academy.AcademyApp
import com.darsequran.academy.data.CurrentEnvironment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Collections
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class CartStoreTest {
    private val app = ApplicationProvider.getApplicationContext<AcademyApp>()
    private val cart get() = app.container.cart
    private val server = MockWebServer()
    private val sent = Collections.synchronizedList(mutableListOf<Int>())
    @Volatile private var failNext = false

    private fun items(quantity: Int) =
        if (quantity == 0) """{"items":[]}""" else """{"items":[{"bookId":"b1","title":"Riyad as-Salihin","quantity":$quantity}]}"""

    @Before fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.method) {
                "GET" -> MockResponse(body = items(1))
                "PATCH" -> {
                    val quantity = Regex("\"quantity\":(\\d+)").find(request.body!!.utf8())!!.groupValues[1].toInt()
                    sent += quantity
                    if (failNext) { failNext = false; MockResponse(code = 409, body = """{"error":"Only 2 in stock."}""") }
                    else MockResponse.Builder().body(items(quantity)).headersDelay(80, TimeUnit.MILLISECONDS).build()
                }
                else -> MockResponse(code = 404)
            }
        }
        server.start()
        CurrentEnvironment.testBaseUrl = server.url("/").toString()
        runBlocking { cart.refresh() }
    }

    @After fun tearDown() { cart.clearLocal(); CurrentEnvironment.testBaseUrl = null; server.close() }

    @Test
    fun quickTapsAllCount() = runBlocking {
        (1..3).map { async(Dispatchers.Default) { cart.change("b1", +1) } }.awaitAll()
        // Taps made while an update is on its way are merged into the next one; nothing is lost or reordered.
        assertEquals(4, sent.last())
        assertEquals(sent.sorted().distinct(), sent.toList())
        assertEquals(4, cart.quantityOf("b1"))
    }

    @Test
    fun aRejectedChangeGoesBackToWhatTheServerHas() = runBlocking {
        failNext = true
        val error = cart.change("b1", +1)
        assertNotNull(error)
        assertEquals(1, cart.quantityOf("b1"))
    }

    @Test
    fun theCapIsEnforcedHere() = runBlocking {
        cart.change("b1", +50)
        assertEquals(listOf(CartStore.MAX_PER_BOOK), sent.toList())
    }

    @Test
    fun goingToZeroRemovesTheLine() = runBlocking {
        cart.change("b1", -1)
        assertEquals(0, cart.quantityOf("b1"))
        assertEquals(emptyList<Any>(), cart.items.value)
    }
}
