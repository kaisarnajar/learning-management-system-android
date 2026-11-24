package com.darsequran.academy.ui.bookstore

import org.junit.Assert.assertEquals
import org.junit.Test

class BookstoreWordsTest {
    @Test
    fun theWebsitesBookStatusWords() {
        assertEquals("Available", bookStatusLabel("AVAILABLE"))
        assertEquals("Out of Stock", bookStatusLabel("OUT_OF_STOCK"))
        assertEquals("Coming Soon", bookStatusLabel("COMING_SOON"))
    }

    @Test
    fun itemsAreCountedAsOnTheWebsite() {
        assertEquals("1 item", itemCount(1))
        assertEquals("0 items", itemCount(0))
        assertEquals("3 items", itemCount(3))
    }

    @Test
    fun anUnverifiedEmailBlocksCheckout() {
        assertEquals(CartCheckout.VerifyEmail, cartCheckout(emailVerified = false, selectedCount = 2))
        assertEquals(CartCheckout.NothingSelected, cartCheckout(emailVerified = true, selectedCount = 0))
        assertEquals(CartCheckout.Ready, cartCheckout(emailVerified = true, selectedCount = 1))
        assertEquals(CartCheckout.Ready, cartCheckout(emailVerified = null, selectedCount = 1)) // the server decides
    }
}
