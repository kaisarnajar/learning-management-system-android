package com.darsequran.academy.ui.common

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.darsequran.academy.ui.pay.readImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A picked payment screenshot, ready to upload ([bytes], [mime]) and to show ([preview]). */
class ProofImage(val bytes: ByteArray, val mime: String, val preview: Bitmap) {
    /** The (bytes, mime) pair the submit calls take. */
    val upload: Pair<ByteArray, String> get() = bytes to mime
}

/** A screenshot attached to a payment or order; see [rememberProofImagePicker]. */
@Stable
class ProofImagePicker internal constructor(private val launch: () -> Unit, private val reset: () -> Unit) {
    var image: ProofImage? by mutableStateOf(null)
        internal set

    fun pick() = launch()
    fun clear() = reset()
}

/**
 * Picks a payment screenshot from the gallery. The chosen image's Uri is saved, so a rotation keeps the attachment
 * (it used to vanish while the typed fields stayed); the bytes and the preview are read again from it, off the main
 * thread.
 */
@Composable
fun rememberProofImagePicker(): ProofImagePicker {
    val context = LocalContext.current
    var uri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { picked -> if (picked != null) uri = picked }
    val picker = remember {
        ProofImagePicker(
            launch = { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            reset = { uri = null },
        )
    }
    LaunchedEffect(uri) {
        val u = uri ?: run { picker.image = null; return@LaunchedEffect }
        picker.image = withContext(Dispatchers.IO) {
            readImage(context, u)?.let { (bytes, mime) ->
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let { ProofImage(bytes, mime, it) }
            }
        }
        if (picker.image == null) {
            uri = null
            Toast.makeText(context, "That image couldn't be read. Try another screenshot.", Toast.LENGTH_LONG).show()
        }
    }
    return picker
}
