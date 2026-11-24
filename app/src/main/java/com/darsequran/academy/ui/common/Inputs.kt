package com.darsequran.academy.ui.common

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.darsequran.academy.ui.theme.Brand

/* Small form inputs shared across the app (they used to live in the pay and courses screens). */

@Composable
fun ChoicePill(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = MaterialTheme.shapes.medium
    Box(
        modifier.clip(shape)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else Brand.outlineSoft, shape)
            .clickable(onClick = onClick).padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface) }
}

@Composable
fun ScreenshotPicker(preview: Bitmap?, onPick: () -> Unit, onClear: () -> Unit) {
    if (preview == null) {
        SecondaryButton("Attach payment screenshot (optional)", onPick)
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(preview.asImageBitmap(), "Payment screenshot", Modifier.size(72.dp).clip(MaterialTheme.shapes.medium), contentScale = ContentScale.Crop)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Screenshot attached", style = MaterialTheme.typography.titleSmall)
                Row {
                    TextButton(onPick) { Icon(Icons.Rounded.AddPhotoAlternate, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Change") }
                    TextButton(onClear) { Text("Remove") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Dropdown(value: String, options: List<String>, modifier: Modifier = Modifier, label: String? = null, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded, { expanded = it }, modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = label?.let { { Text(it) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            singleLine = true,
        )
        ExposedDropdownMenu(expanded, { expanded = false }) {
            options.forEachIndexed { i, option -> DropdownMenuItem({ Text(option) }, { onSelect(i); expanded = false }) }
        }
    }
}

@Composable
fun SearchField(initial: String, placeholder: String, onSearch: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf(initial) }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Rounded.Search, null) },
        trailingIcon = { if (text.isNotEmpty()) IconButton({ text = ""; onSearch("") }) { Icon(Icons.Rounded.Close, "Clear") } },
        singleLine = true,
        shape = MaterialTheme.shapes.extraLarge,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch(text.trim()) }),
    )
}
