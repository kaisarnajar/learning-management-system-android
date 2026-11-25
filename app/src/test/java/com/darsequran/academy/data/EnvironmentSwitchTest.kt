package com.darsequran.academy.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.darsequran.academy.AcademyApp
import com.darsequran.academy.data.remote.SessionUser
import com.darsequran.academy.data.remote.TokenResponse
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/** Switching servers in the debug build keeps a separate sign-in on each. */
@RunWith(AndroidJUnit4::class)
class EnvironmentSwitchTest {
    private val container = ApplicationProvider.getApplicationContext<AcademyApp>().container

    @After fun tearDown() = runBlocking {
        AppEnvironment.available.forEach { container.switchEnvironment(it); container.sessionStore.clear() }
        container.switchEnvironment(AppEnvironment.default)
    }

    @Test
    fun eachServerKeepsItsOwnSignIn() = runBlocking {
        val staging = AppEnvironment.STAGING
        val production = AppEnvironment.PRODUCTION
        container.switchEnvironment(staging)
        val user = SessionUser("u-staging", "s@example.test", role = "USER")
        container.sessionStore.save(TokenResponse("a", "r", user = user))

        container.switchEnvironment(production)
        assertEquals(production, CurrentEnvironment.value)
        assertEquals(production, container.environments.environment.value)
        assertNull("production has no sign-in yet", container.sessionStore.session.value)

        container.switchEnvironment(staging)
        assertEquals("u-staging", container.sessionStore.session.value?.user?.id)
        assertEquals(staging, container.sessionStore.session.value?.env)
    }
}
