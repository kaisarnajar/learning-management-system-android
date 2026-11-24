package com.darsequran.academy.ui.pay

import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import androidx.activity.ComponentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.darsequran.academy.data.remote.PaymentSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf

/** launchUpi is called from a screen, so the tests use an Activity. */
@RunWith(AndroidJUnit4::class)
class LaunchUpiTest {
    private val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
    private val settings = PaymentSettings(upiId = "academy@upi")

    @Test
    fun withoutAUpiAppItSaysSoInsteadOfOpeningAnEmptyChooser() {
        assertFalse(launchUpi(activity, settings, 50000, "Course fee"))
        assertNull(shadowOf(activity).nextStartedActivity)
    }

    @Test
    fun withAUpiAppItOpensIt() {
        val upiApp = ComponentName("com.example.upi", "com.example.upi.Pay")
        shadowOf(activity.packageManager).apply {
            addActivityIfNotPresent(upiApp)
            addIntentFilterForActivity(upiApp, IntentFilter(Intent.ACTION_VIEW).apply { addDataScheme("upi"); addCategory(Intent.CATEGORY_DEFAULT) })
        }
        assertTrue(launchUpi(activity, settings, 50000, "Course fee"))
        assertEquals(Intent.ACTION_CHOOSER, shadowOf(activity).nextStartedActivity.action)
    }
}
