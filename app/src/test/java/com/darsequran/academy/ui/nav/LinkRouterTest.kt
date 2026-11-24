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
        assertEquals(HrefDestination(LibraryItemRoute("l1")), to("/library/l1"))
        assertEquals(HrefDestination(TeacherRoute("t1")), to("/teachers/t1"))
    }

    @Test
    fun theAskFormIsNotAFatwaCalledAsk() {
        assertEquals(HrefDestination(AskFatwaRoute), to("/fatwa/ask"))
    }

    @Test
    fun bookstoreLinksOpenTheBookOrTheOrders() {
        assertEquals(HrefDestination(BookRoute("b1")), to("/bookstore/b1"))
        assertEquals(HrefDestination(OrdersRoute), to("/bookstore/orders/o1"))
        assertEquals(HrefDestination(OrdersRoute), to("/bookstore/orders"))
        assertEquals(HrefDestination(BookstoreRoute), to("/bookstore"))
    }

    @Test
    fun theStudentsOwnPages() {
        assertEquals(HrefDestination(LearnRoute("c1", "Course")), to("/profile/courses/c1"))
        assertEquals(HrefDestination(LearnRoute("c1", "Course")), to("/profile/courses/c1/attendance"))
        assertEquals(HrefDestination(CoursesTab, isTab = true), to("/profile/courses"))
        assertEquals(HrefDestination(PaymentsRoute), to("/profile/payments"))
        assertEquals(HrefDestination(OrdersRoute), to("/profile/cart"))
        assertEquals(HrefDestination(WaiversRoute), to("/profile/waiver-requests"))
        assertEquals(HrefDestination(ReviewsRoute), to("/profile/reviews"))
        assertEquals(HrefDestination(NotificationsRoute), to("/profile/notifications"))
        assertEquals(HrefDestination(AccountTab, isTab = true), to("/profile"))
        assertEquals(HrefDestination(AccountTab, isTab = true), to("/profile/"))
    }

    @Test
    fun pagesTheAppDoesNotHave() {
        assertNull(to("/about-us"))
        assertNull(to("/blog"))
        assertNull(to("/library"))
        assertNull(to("/"))
    }
}
