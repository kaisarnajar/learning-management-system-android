package com.darsequran.academy.ui.nav

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.shadows.ShadowLooper

@RunWith(AndroidJUnit4::class)
class GraphScopeTest {
    class ScreenViewModel : ViewModel() {
        var cleared = false
        override fun onCleared() { cleared = true }
    }

    private var created = mutableListOf<ScreenViewModel>()

    private fun ComponentActivity.show(graph: () -> String) = setContent {
        val k = graph()
        key(k) { GraphScope(k) { created += viewModel<ScreenViewModel>(key = "screen") } }
    }

    @Test
    fun aReplacedGraphsViewModelsAreCleared() {
        var user by mutableStateOf("amina")
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        activity.show { user }
        ShadowLooper.idleMainLooper()
        val first = created.last()
        user = "bilal"
        ShadowLooper.idleMainLooper()
        assertEquals(true, first.cleared)
        assertEquals(false, created.last().cleared)
    }

    @Test
    fun rotationKeepsThem() {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        controller.get().show { "amina" }
        ShadowLooper.idleMainLooper()
        val before = created.last()
        controller.recreate()
        controller.get().show { "amina" }
        ShadowLooper.idleMainLooper()
        assertEquals(false, before.cleared)
        assertSame(before, created.last())
    }
}
