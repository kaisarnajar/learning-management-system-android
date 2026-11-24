package com.darsequran.academy.ui.pay

import com.darsequran.academy.data.remote.PaymentSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The pay screen follows the website's EnrollmentPaymentForm / MonthlyPaymentForm / PaymentDetailsPanel. */
class PayWordingTest {
    @Test
    fun submitButtonsUseTheWebsitesWording() {
        assertEquals("Submit enrollment fee for verification", paySubmitLabel("enrollment", free = false))
        assertEquals("Claim Fee Waiver & Enroll", paySubmitLabel("enrollment", free = true))
        assertEquals("Submit fee payment for verification", paySubmitLabel("course", free = false))
        assertEquals("Claim Fee Waiver", paySubmitLabel("course", free = true))
    }

    @Test
    fun anyFilledInReferenceIsEnough() {
        assertTrue(canSubmitPayment(free = false, reference = "A1"))
        assertFalse(canSubmitPayment(free = false, reference = ""))
        assertFalse(canSubmitPayment(free = false, reference = "  "))
        assertTrue(canSubmitPayment(free = true, reference = ""))
    }

    @Test
    fun theUpiNoteMatchesTheWebsitesForms() {
        assertEquals("Tajweed enrollment", feePaymentNote("enrollment", "Tajweed", null))
        assertEquals("Tajweed — Quarterly", feePaymentNote("course", "Tajweed", "Quarterly"))
        assertEquals("Tajweed — Monthly", feePaymentNote("course", "Tajweed", null))
        assertEquals(80, feePaymentNote("enrollment", "x".repeat(100), null).length)
    }

    @Test
    fun theQrCodeHoldsTheWebsitesUpiLink() {
        val settings = PaymentSettings(upiId = "academy@upi", upiPayeeName = "Darse Quran")
        assertEquals(
            "upi://pay?pa=academy%40upi&pn=Darse+Quran&am=500.00&cu=INR&tn=Tajweed+enrollment&tr=FEE-50000",
            upiQrUri(settings, 50_000, "Tajweed enrollment"),
        )
        assertEquals("upi://pay?pa=academy%40upi&pn=Darse+Quran&cu=INR", upiQrUri(settings, null, null))
        assertEquals("upi://pay?pa=academy%40upi&pn=Darse+Quran&cu=INR", upiQrUri(settings, 0, "x"))
        assertTrue(upiQrUri(settings, 100, "n", paymentRef = "ORDER-7").endsWith("&tr=ORDER-7"))
        assertEquals(
            "upi://pay?pa=a%40b&pn=Darse+Quran+Academy&cu=INR",
            upiVpaUri(PaymentSettings(upiId = "a@b", upiPayeeName = " ")),
        )
    }

    @Test
    fun theQrCodeHasTheWebsitesQuietZone() {
        val matrix = qrMatrix("upi://pay?pa=academy%40upi&pn=Darse+Quran&cu=INR")
        assertEquals(matrix.width, matrix.height)
        // Two light modules on every side, then the finder pattern's dark corner.
        assertFalse(matrix[1, 1])
        assertTrue(matrix[2, 2])
    }
}
