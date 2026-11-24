package com.darsequran.academy.ui.manage.content

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.darsequran.academy.data.staff.PickOutcome
import com.darsequran.academy.data.staff.PickedFile
import com.darsequran.academy.data.staff.UploadLimit
import com.darsequran.academy.data.staff.content.ContentImage
import com.darsequran.academy.data.staff.readPicked
import com.darsequran.academy.ui.common.RemoteImage
import com.darsequran.academy.ui.common.openWeb
import kotlinx.coroutines.launch

/** One entry of a row's "more" menu. */
data class RowAction(val label: String, val destructive: Boolean = false, val onClick: () -> Unit)

/** The ⋮ button of a list row; hidden when there is nothing to offer. */
@Composable
fun RowMenu(actions: List<RowAction>) {
    if (actions.isEmpty()) return
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.Rounded.MoreVert, "More actions") }
        DropdownMenu(open, { open = false }) {
            actions.forEach { action ->
                DropdownMenuItem(
                    text = { Text(action.label, color = if (action.destructive) MaterialTheme.colorScheme.error else Color.Unspecified) },
                    onClick = { open = false; action.onClick() },
                )
            }
        }
    }
}

/** A small picture at the start of a list row. */
@Composable
fun RowThumbnail(path: String?, width: Int = 48, height: Int = 48) {
    RemoteImage(path, null, Modifier.size(width.dp, height.dp).clip(RoundedCornerShape(8.dp)))
}

/** "View on website" for a public page path such as /blog/abc. */
@Composable
fun ViewOnWebsiteButton(path: String?, label: String = "View on website") {
    if (path.isNullOrBlank()) return
    val context = LocalContext.current
    OutlinedButton(onClick = { openWeb(context, path) }) {
        Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, Modifier.size(18.dp))
        Text("  $label")
    }
}

/**
 * The images of a post or announcement, as on the website's forms: the saved ones (tap ✕ to mark one for removal,
 * sent as `removeImage`), and new ones picked from the gallery (sent as `images`), up to [max] in all.
 * [readOnly] shows the saved images only.
 */
@Composable
fun ImagesField(
    label: String,
    current: List<ContentImage>,
    removed: Set<String>,
    onRemovedChange: (Set<String>) -> Unit,
    picked: List<PickedFile>,
    onPickedChange: (List<PickedFile>) -> Unit,
    max: Int,
    readOnly: Boolean = false,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val slots = (max - (current.size - removed.size) - picked.size).coerceAtLeast(0)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(maxOf(2, max))) { uris: List<Uri> ->
        scope.launch {
            val outcomes = uris.take(slots).map { context.readPicked(it, UploadLimit.Image) }
            onPickedChange(picked + outcomes.filterIsInstance<PickOutcome.Picked>().map { it.file })
            val problems = buildList {
                if (uris.size > slots) add("Only $slots more photo${if (slots == 1) "" else "s"} fit, so ${uris.size - slots} weren't added.")
                outcomes.filterIsInstance<PickOutcome.Rejected>().forEach { add(it.reason) }
            }
            if (problems.isNotEmpty()) android.widget.Toast.makeText(context, problems.distinct().joinToString("\n"), android.widget.Toast.LENGTH_LONG).show()
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        current.forEach { img ->
            val gone = img.id in removed
            Box(Modifier.fillMaxWidth().height(170.dp).clip(MaterialTheme.shapes.large)) {
                RemoteImage(img.imagePath, img.caption, Modifier.fillMaxSize().alpha(if (gone) 0.3f else 1f), contentScale = ContentScale.Crop)
                if (!readOnly) {
                    TextButton(
                        onClick = { onRemovedChange(if (gone) removed - img.id else removed + img.id) },
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                    ) {
                        Icon(if (gone) Icons.AutoMirrored.Rounded.Undo else Icons.Rounded.Close, null, tint = MaterialTheme.colorScheme.error)
                        Text(if (gone) " Keep" else " Remove", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        if (!readOnly) {
            picked.forEachIndexed { index, file ->
                Box(Modifier.fillMaxWidth().height(170.dp).clip(MaterialTheme.shapes.large)) {
                    AsyncImage(file.bytes, file.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    TextButton(onClick = { onPickedChange(picked.filterIndexed { i, _ -> i != index }) }, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                        Icon(Icons.Rounded.Close, null, tint = MaterialTheme.colorScheme.error)
                        Text(" Remove", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    enabled = slots > 0,
                ) {
                    Icon(Icons.Rounded.AddPhotoAlternate, null, Modifier.size(18.dp))
                    Text(if (current.isEmpty()) "  Add images" else "  Add more images")
                }
            }
            Text(
                "JPEG, PNG, WebP or GIF, up to 5 MB each. " +
                    if (slots > 0) "You can add $slots more (max $max in all)." else "This holds the maximum of $max images.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
