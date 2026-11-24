package com.darsequran.academy.ui.manage.common

import androidx.activity.ComponentActivity
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.darsequran.academy.AcademyApp
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class ActionRunnerTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun twoTapsInOneFrameWriteOnce() {
        // A dispatcher that doesn't run anything until asked: both taps happen before the first write starts.
        val dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        try {
            val runner = ActionRunner(ApplicationProvider.getApplicationContext<AcademyApp>().container)
            val writes = AtomicInteger()
            runner.run({ writes.incrementAndGet() })
            runner.run({ writes.incrementAndGet() })
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals(1, writes.get())
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun aWriteFinishesAndReportsAfterRecreation() {
        val gate = CompletableDeferred<Unit>()
        val restore = StateRestorationTester(compose)
        restore.setContent {
            val runner = rememberActionRunner()
            Column {
                Button(onClick = { runner.run({ gate.await() }, success = "Payment recorded.") }) { Text("Record") }
                SnackbarHost(runner.snackbar)
            }
        }
        compose.onNodeWithText("Record").performClick()
        restore.emulateSavedInstanceStateRestore()   // the screen is rebuilt while the write is in flight
        gate.complete(Unit)
        compose.waitUntil(5_000) { compose.onAllNodes(hasText("Payment recorded.")).fetchSemanticsNodes().isNotEmpty() }
    }
}
