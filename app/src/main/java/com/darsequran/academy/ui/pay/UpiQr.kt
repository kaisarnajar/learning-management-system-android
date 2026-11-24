package com.darsequran.academy.ui.pay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.darsequran.academy.data.remote.PaymentSettings
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.net.URLEncoder
import java.util.Locale

/*
 * The UPI QR code of the website's PaymentDetailsPanel: the same upi://pay link (services/upi.ts), drawn with the
 * same quiet zone and colours as its `qrcode` call (margin 2, #1c1917 on white, error correction M).
 */

/** The payee name the website falls back to when the academy has not set one. */
private const val DEFAULT_PAYEE = "Darse Quran Academy"
private const val PAYEE_MAX = 50
private const val NOTE_MAX = 80
private const val REF_MAX = 35
private const val QR_MARGIN = 2
private const val PAISE_PER_RUPEE = 100.0
private const val QR_DARK_ARGB = 0xFF1C1917
private const val QR_SIZE = 220

/** Overlap between modules so no hairline seams show between neighbours. */
private const val SEAM = 0.5f

/** `upi://pay` for the academy's UPI ID only, as the website's buildUpiVpaUrlFromSettings. */
fun upiVpaUri(settings: PaymentSettings): String = upiUri(
    "pa" to settings.upiId.orEmpty(),
    "pn" to payee(settings).take(PAYEE_MAX),
    "cu" to "INR",
)

/**
 * `upi://pay` with the amount, as the website's buildUpiPaymentUrlFromSettings. Without a [paymentRef] the
 * transaction reference is `FEE-<amount in paise>`, as the website's panel sends it.
 */
fun upiPaymentUri(
    settings: PaymentSettings,
    amountPaise: Int,
    note: String,
    paymentRef: String? = null,
): String = upiUri(
    "pa" to settings.upiId.orEmpty(),
    "pn" to payee(settings).take(PAYEE_MAX),
    "am" to "%.2f".format(Locale.ROOT, amountPaise / PAISE_PER_RUPEE),
    "cu" to "INR",
    "tn" to note.take(NOTE_MAX),
    "tr" to (paymentRef ?: "FEE-$amountPaise").take(REF_MAX),
)

/** The link the QR code holds: with the amount when one is due, else just the UPI ID (as the website decides). */
fun upiQrUri(settings: PaymentSettings, amountPaise: Int?, note: String?, paymentRef: String? = null): String =
    if (amountPaise != null && amountPaise > 0) upiPaymentUri(settings, amountPaise, note.orEmpty(), paymentRef)
    else upiVpaUri(settings)

private fun payee(settings: PaymentSettings) =
    settings.upiPayeeName?.trim()?.takeIf { it.isNotEmpty() } ?: DEFAULT_PAYEE

/** Form-encodes like the browser's URLSearchParams, so the QR holds the same text as the website's. */
private fun upiUri(vararg params: Pair<String, String>): String =
    "upi://pay?" + params.joinToString("&") { (k, v) -> "$k=${URLEncoder.encode(v, "UTF-8")}" }

/** The QR code's modules for [text], with the website's quiet zone. */
fun qrMatrix(text: String): BitMatrix = QRCodeWriter().encode(
    text,
    BarcodeFormat.QR_CODE,
    0,
    0,
    mapOf(EncodeHintType.MARGIN to QR_MARGIN, EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M),
)

private val QrDark = Color(QR_DARK_ARGB)

/** Draws [text] as a QR code, dark modules on white whatever the theme, so every UPI app can scan it. */
@Suppress("FunctionNaming") // a composable, named like one
@Composable
fun QrCode(text: String, description: String, modifier: Modifier = Modifier, size: Dp = QR_SIZE.dp) {
    val matrix = remember(text) { qrMatrix(text) }
    Canvas(modifier.size(size).background(Color.White).semantics { contentDescription = description }) {
        val cell = this.size.width / matrix.width
        for (y in 0 until matrix.height) for (x in 0 until matrix.width) {
            if (matrix[x, y]) drawRect(QrDark, Offset(x * cell, y * cell), Size(cell + SEAM, cell + SEAM))
        }
    }
}
