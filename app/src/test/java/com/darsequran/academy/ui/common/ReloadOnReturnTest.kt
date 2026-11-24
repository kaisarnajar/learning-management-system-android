package com.darsequran.academy.ui.common

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.shadows.ShadowLooper
import androidx.navigation.compose.composable

@RunWith(AndroidJUnit4::class)
class ReloadOnReturnTest {
    private var reloads = 0
    private fun ComponentActivity.show() = setContent { ReloadOnReturn { reloads++ } }

    @Test
    fun comingBackFromAnotherAppReloads() {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        controller.get().show(); ShadowLooper.idleMainLooper()
        assertEquals("not on first display", 0, reloads)
        controller.pause().stop(); controller.restart().start().resume(); ShadowLooper.idleMainLooper()
        assertEquals(1, reloads)
    }

    @Test
    fun aRotationDoesNotReload() {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        controller.get().show(); ShadowLooper.idleMainLooper()
        controller.recreate()
        controller.get().show(); ShadowLooper.idleMainLooper()
        assertEquals(0, reloads)
    }

    @Test
    fun comingBackFromAPushedScreenReloads() {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        lateinit var nav: androidx.navigation.NavHostController
        controller.get().setContent {
            nav = androidx.navigation.compose.rememberNavController()
            androidx.navigation.compose.NavHost(nav, startDestination = "list") {
                composable("list") { ReloadOnReturn { reloads++ } }
                composable("detail") { }
            }
        }
        ShadowLooper.idleMainLooper()
        nav.navigate("detail"); ShadowLooper.idleMainLooper()
        nav.popBackStack(); ShadowLooper.idleMainLooper()
        assertEquals(1, reloads)
    }
}
