package com.darsequran.academy.ui.nav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LinkRouterTest {
    private fun to(href: String) = routeForHref(href, Shell.Student)

    @Test
    fun publicPagesOpenTheirScreens() {
        assertEquals(HrefDestination(BlogPostRoute("patience")), to("/blog/patience"))
        assertEquals(HrefDestination(FatwaDetailRoute("q1")), to("/fatwa/q1#answer"))
        assertEquals(HrefDestination(AnnouncementRoute("a1")), to("/announcements/a1"))
        assertEquals(HrefDestination(CourseRoute("c1")), to("/courses/c1?ref=home"))
    }

    @Test
    fun theStudentsOwnPages() {
        assertEquals(HrefDestination(LearnRoute("c1", "Course")), to("/profile/courses/c1"))
        assertEquals(HrefDestination(PaymentsRoute), to("/profile/payments"))
        assertEquals(HrefDestination(OrdersRoute), to("/profile/cart"))
        assertEquals(HrefDestination(OrdersRoute), to("/bookstore/orders/o1"))
        assertEquals(HrefDestination(WaiversRoute), to("/profile/waiver-requests"))
        assertEquals(HrefDestination(CoursesTab, isTab = true), to("/profile"))
    }

    @Test
    fun pagesTheAppDoesNotHave() {
        assertNull(to("/about-us"))
        assertNull(to("/blog"))
        assertNull(to("/"))
    }
}
