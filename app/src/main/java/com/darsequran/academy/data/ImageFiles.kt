package com.darsequran.academy.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Reads a picked photo as a JPEG no larger than [maxSide] pixels on its longer side, upright.
 *
 * Phone photos can be 50+ megapixels: decoding one at full size needs hundreds of MB and fails. The photo is first
 * measured, then decoded already shrunk (a power-of-two sample size), then scaled and turned to match its EXIF
 * orientation, so a portrait shot isn't uploaded sideways. Returns null when the image can't be read.
 */
suspend fun Context.readImageAsJpeg(uri: Uri, maxSide: Int = 1600): ByteArray? = withContext(Dispatchers.IO) {
    runCatching {
        val resolver = contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        // Measuring only: decodeStream returns null here by design, the size lands in `bounds`.
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxSide) }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return@runCatching null
        // Orientation is a nicety: a format or file whose EXIF can't be read is used as it is.
        val rotation = runCatching { resolver.openInputStream(uri)?.use(::exifRotation) }.getOrNull() ?: 0
        val scale = minOf(1f, maxSide.toFloat() / maxOf(decoded.width, decoded.height))
        val matrix = Matrix().apply { postScale(scale, scale); postRotate(rotation.toFloat()) }
        val upright = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (upright !== decoded) decoded.recycle()
        ByteArrayOutputStream().use { out ->
            upright.compress(Bitmap.CompressFormat.JPEG, 85, out)
            upright.recycle()
            out.toByteArray()
        }
    }.getOrNull()
}

/** The largest power-of-two shrink that keeps the longer side at least [maxSide] (BitmapFactory's inSampleSize). */
fun sampleSizeFor(width: Int, height: Int, maxSide: Int): Int {
    var sample = 1
    while (maxOf(width, height) / (sample * 2) >= maxSide) sample *= 2
    return sample
}

private fun exifRotation(input: InputStream): Int = when (ExifInterface(input).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
    ExifInterface.ORIENTATION_ROTATE_90 -> 90
    ExifInterface.ORIENTATION_ROTATE_180 -> 180
    ExifInterface.ORIENTATION_ROTATE_270 -> 270
    else -> 0
}
