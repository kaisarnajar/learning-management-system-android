package com.darsequran.academy.ui.bookstore

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.darsequran.academy.AcademyApp
import com.darsequran.academy.data.CurrentEnvironment
import com.darsequran.academy.data.remote.SessionUser
import com.darsequran.academy.data.remote.TokenResponse
import com.darsequran.academy.testing.MainDispatcherRule
import com.darsequran.academy.ui.common.Load
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Collections

@RunWith(AndroidJUnit4::class)
class CheckoutViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val app = ApplicationProvider.getApplicationContext<AcademyApp>()
    private val server = MockWebServer()
    private val orders = Collections.synchronizedList(mutableListOf<String>())
    private val cartPatches = Collections.synchronizedList(mutableListOf<String>())
    @Volatile private var cartQuantity = 1
    @Volatile private var secondBook = false

    @Before fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.url.encodedPath) {
                "/api/v1/bookstore/cart" -> {
                    if (request.method == "PATCH") cartPatches += request.body!!.utf8()
                    MockResponse(body = cartBody())
                }
                "/api/v1/bookstore/checkout-quote" -> MockResponse(body = """{"lines":[],"totalAmountInrPaise":${cartQuantity * 40000}}""")
                "/api/v1/bookstore/orders" -> { orders += request.body!!.utf8(); MockResponse(body = """{"success":true}""") }
                else -> MockResponse(code = 404)
            }
        }
        server.start()
        CurrentEnvironment.testBaseUrl = server.url("/").toString()
        runBlocking {
            app.container.sessionStore.save(TokenResponse("a", "r", user = SessionUser("u1", "student@example.test")))
            withTimeout(5_000) { while (app.container.cart.quantityOf("b1") != 1) delay(20) }
        }
    }

    private fun cartBody(): String {
        val b1 = """{"bookId":"b1","title":"Riyad as-Salihin","quantity":$cartQuantity}"""
        val b2 = """{"bookId":"b2","title":"Bulugh al-Maram","quantity":2}"""
        return """{"items":[$b1${if (secondBook) ",$b2" else ""}]}"""
    }

    @After fun tearDown() {
        runBlocking {
            app.container.sessionStore.clear()
            // The container empties the cart (and its selection) when it sees the sign-out, on its own coroutine. Wait
            // for that here: otherwise it can land in the next test, after that test has already chosen its books.
            withTimeout(5_000) { while (app.container.cart.items.value.isNotEmpty()) delay(20) }
        }
        CurrentEnvironment.testBaseUrl = null
        server.close()
    }

    private fun vm() = CheckoutViewModel(app.container.repo, app.container.cart)

    @Test
    fun theQuotedCartIsOrdered() = runBlocking {
        val vm = vm()
        withTimeout(5_000) { vm.state.first { it.quote is Load.Ready } }
        vm.place("upi", "412788901234", "House 1, Srinagar", "190001", "9999999999", null)
        withTimeout(5_000) { vm.state.first { it.done } }
        assertEquals(1, orders.size)
        assertTrue(orders[0], orders[0].contains("""{"bookId":"b1","quantity":1}"""))
    }

    @Test
    fun aCartChangedAfterTheQuoteIsNotOrdered() = runBlocking {
        val vm = vm()
        withTimeout(5_000) { vm.state.first { it.quote is Load.Ready } }
        cartQuantity = 2
        app.container.cart.refresh() // e.g. changed on the website meanwhile
        vm.place("upi", "412788901234", "House 1, Srinagar", "190001", "9999999999", null)
        withTimeout(5_000) { vm.state.first { it.error != null } }
        withTimeout(5_000) { vm.state.first { (it.quote as? Load.Ready)?.value?.totalAmountInrPaise == 80000 } }
        delay(200)
        assertEquals(0, orders.size)
    }

    @Test
    fun theOrderCarriesTheTotalShown() = runBlocking {
        val vm = vm()
        withTimeout(5_000) { vm.state.first { it.quote is Load.Ready } }
        vm.place("upi", "412788901234", "House 1, Srinagar", "190001", "9999999999", null)
        withTimeout(5_000) { vm.state.first { it.done } }
        assertTrue(orders[0], orders[0].contains("expectedTotalPaise"))
        assertTrue(orders[0], orders[0].contains("40000"))
        assertFalse(orders[0], orders[0].contains("notes"))
    }

    @Test
    fun onlyTheSelectedBooksAreOrderedAndTheRestStayInTheCart() = runBlocking {
        secondBook = true
        app.container.cart.refresh()
        app.container.cart.select("b2", false)
        val vm = vm()
        withTimeout(5_000) { vm.state.first { it.quote is Load.Ready } }
        vm.place("upi", "412788901234", "House 1, Srinagar", "190001", "9999999999", null)
        withTimeout(5_000) { vm.state.first { it.done } }
        // The book id with its key: the multipart boundary is random and may contain "b2" by chance.
        assertFalse(orders[0], orders[0].contains(""""bookId":"b2""""))
        assertEquals(listOf("""{"bookId":"b2","quantity":2}"""), cartPatches.toList())
        assertTrue(app.container.cart.unselected.value.isEmpty())
    }
}
