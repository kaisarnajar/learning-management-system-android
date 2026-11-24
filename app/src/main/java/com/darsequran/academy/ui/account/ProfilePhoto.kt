package com.darsequran.academy.ui.account

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.darsequran.academy.data.remote.Profile
import com.darsequran.academy.ui.common.Avatar
import com.darsequran.academy.ui.common.LoadingBox
import com.darsequran.academy.ui.common.readImage

/**
 * The avatar with its camera button and the website's hint. As on the website, a photo can be chosen only
 * once the (unsaved) gender is Male.
 */
@Composable
fun ProfilePhoto(p: Profile, gender: String, busy: Boolean, onPhoto: (ByteArray, String) -> Unit) {
    val context = LocalContext.current
    var picked by remember { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { picked = it }
    LaunchedEffect(picked) {
        val uri = picked ?: return@LaunchedEffect
        val image = readImage(context, uri)
        if (image != null) {
            onPhoto(image.first, image.second)
        } else {
            Toast.makeText(context, "That photo couldn't be read. Try another one.", Toast.LENGTH_LONG).show()
        }
        // Forgotten once handed on (last: changing the key ends this effect), so picking the same photo again after
        // a failed upload still uploads it.
        picked = null
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            Avatar(p.name, p.image, 96.dp)
            if (photoAllowed(gender)) {
                IconButton(
                    { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    enabled = !busy, // one upload at a time
                    modifier = Modifier.align(Alignment.BottomEnd).size(36.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                ) {
                    if (busy) {
                        LoadingBox(Modifier.size(18.dp))
                    } else {
                        val tint = MaterialTheme.colorScheme.onPrimary
                        Icon(Icons.Rounded.CameraAlt, "Change photo", tint = tint, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        Text(
            photoHint(gender.ifBlank { null }),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
