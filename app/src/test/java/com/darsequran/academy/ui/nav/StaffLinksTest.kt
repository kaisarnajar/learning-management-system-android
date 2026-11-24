package com.darsequran.academy.ui.nav

import com.darsequran.academy.ui.manage.TeachTab
import com.darsequran.academy.ui.manage.courses.Portal
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
        assertEquals(HrefDestination(routeForAdminHref(adminPage)!!), routeForHref(adminPage, Shell.Admin))
        assertNull(routeForHref(adminPage, Shell.Student))
        assertNull(routeForHref(adminPage, Shell.Teacher))
    }

    @Test
    fun teacherLinksOpenTheTeachingTabOnlyForTeachers() {
        assertEquals(HrefDestination(TeachTab, isTab = true), routeForHref("/teacher/courses/c1/attendance", Shell.Teacher))
        assertNull(routeForHref("/teacher/courses/c1/attendance", Shell.Student))
    }

    @Test
    fun anUnknownPortalIsTheTeacherOne() {
        assertEquals(Portal.Admin, Portal.of("admin"))
        assertEquals(Portal.Teacher, Portal.of("teacher"))
        assertEquals(Portal.Teacher, Portal.of("superuser"))
    }
}
