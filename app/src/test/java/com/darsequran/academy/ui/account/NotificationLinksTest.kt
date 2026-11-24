package com.darsequran.academy.ui.account

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationLinksTest {
    @Test
    fun eachDestinationIsLabelledAsOnTheWebsite() {
        assertEquals("View in Payments", relatedPageLabel("/profile/payments?tab=history"))
        assertEquals("View announcement", relatedPageLabel("/courses/c1/announcements"))
        assertEquals("View in My Courses", relatedPageLabel("/profile/courses"))
        assertEquals("View order", relatedPageLabel("/profile/cart"))
        assertEquals("View order", relatedPageLabel("/bookstore/orders/o1"))
        assertEquals("View blog post", relatedPageLabel("/blog/a-post"))
        assertEquals("View related page", relatedPageLabel("/courses/c1"))
    }

    @Test
    fun theNotificationsListItselfAndAnEmptyLinkGetNoLabel() {
        assertNull(relatedPageLabel(""))
        assertNull(relatedPageLabel("/profile/notifications"))
    }

    @Test
    fun anAbsoluteLinkIsReadByItsPath() =
        assertEquals("View in Payments", relatedPageLabel("https://academy.example/profile/payments"))
}
