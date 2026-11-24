package com.darsequran.academy.ui.nav

import com.darsequran.academy.ui.manage.AdminDashboardTab
import com.darsequran.academy.ui.manage.TeachTab
import com.darsequran.academy.data.staff.courses.Portal
import com.darsequran.academy.ui.manage.routeForAdminHref
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class StaffLinksTest {
    private val adminPage = "/admin/students"

    @Test
    fun adminLinksOpenOnlyForAdmins() {
        assertNotNull("the sample must be a page the app has", routeForAdminHref(adminPage))
        val expected = HrefDestination(routeForAdminHref(adminPage)!!, underTab = AdminDashboardTab)
        assertEquals("opens inside the Admin tab", expected, routeForHref(adminPage, Shell.Admin))
        assertEquals(expected, routeForHref(adminPage, Shell.Developer))
        assertEquals(HrefDestination(AdminDashboardTab, isTab = true), routeForHref("/admin", Shell.Admin))
        assertNull(routeForHref(adminPage, Shell.Student))
        assertNull(routeForHref(adminPage, Shell.Teacher))
    }

    @Test
    fun teacherLinksOpenTheTeachingTabOnlyForTeachers() {
        assertEquals(HrefDestination(TeachTab, isTab = true), routeForHref("/teacher/courses/c1/attendance", Shell.Teacher))
        assertNull(routeForHref("/teacher/courses/c1/attendance", Shell.Student))
    }

    @Test
    fun everyShellHasThePublicTabsAndStartsOnHome() {
        Shell.entries.forEach { shell ->
            assertEquals(HomeTab, shell.start)
            assertEquals(listOf("Home", "Courses", "Explore"), shell.tabs.take(3).map { it.label })
            assertEquals("Account", shell.tabs.last().label)
        }
        assertEquals(listOf(4, 5, 5, 5), Shell.entries.map { it.tabs.size })
        assertEquals("Teach", Shell.of("TEACHER").tabs[3].label)
        assertEquals("Admin", Shell.of("ADMIN").tabs[3].label)
        assertEquals("Developer", Shell.of("DEVELOPER").tabs[3].label)
        assertEquals(Shell.Student, Shell.of("USER"))
    }

    @Test
    fun anUnknownPortalIsTheTeacherOne() {
        assertEquals(Portal.Admin, Portal.of("admin"))
        assertEquals(Portal.Teacher, Portal.of("teacher"))
        assertEquals(Portal.Teacher, Portal.of("superuser"))
    }
}
