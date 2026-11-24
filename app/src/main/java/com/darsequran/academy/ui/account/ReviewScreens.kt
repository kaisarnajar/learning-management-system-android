package com.darsequran.academy.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.darsequran.academy.data.remote.Review
import com.darsequran.academy.ui.common.AppCard
import com.darsequran.academy.ui.common.BackTopBar
import com.darsequran.academy.ui.common.Banner
import com.darsequran.academy.ui.common.Load
import com.darsequran.academy.ui.common.LoadContent
import com.darsequran.academy.ui.common.Overline
import com.darsequran.academy.ui.common.PrimaryButton
import com.darsequran.academy.ui.common.StatusChip
import com.darsequran.academy.ui.common.Tone
import com.darsequran.academy.ui.common.formatDate
import com.darsequran.academy.ui.common.vmFactory
import com.darsequran.academy.ui.theme.Brand

@Composable
fun ReviewsScreen(onBack: () -> Unit) {
    val vm: ReviewsViewModel = viewModel(factory = vmFactory { ReviewsViewModel(it.repo, it.auth) })
    val state by vm.state.collectAsStateWithLifecycle()
    Scaffold(topBar = { BackTopBar("My reviews", onBack) }) { padding ->
        Box(Modifier.padding(padding)) {
            LoadContent(state, vm::load) { reviews ->
                LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Text(
                            "Share your learning experience. Approved reviews may appear on the academy homepage.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    item { Overline("Write a review") }
                    item { ReviewForm(vm) }
                    if (reviews.isNotEmpty()) item { Spacer(Modifier.height(8.dp)); Overline("Your submissions") }
                    items(reviews, key = { it.id }) { ReviewCard(it) }
                }
            }
        }
    }
}

@Composable
private fun ReviewForm(vm: ReviewsViewModel) {
    val draft by vm.draft.collectAsStateWithLifecycle()
    val sending by vm.sending.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    // The website shows a field's message once the field has been touched.
    var touched by remember { mutableStateOf(emptySet<ReviewField>()) }
    val errors = reviewErrors(draft)
    fun shown(field: ReviewField) = errors[field]?.takeIf { field in touched }
    AppCard(Modifier.fillMaxWidth()) {
        Text("Your rating *", style = MaterialTheme.typography.labelLarge)
        StarPicker(draft.rating) { rating ->
            vm.edit { it.copy(rating = rating) }
            touched = touched + ReviewField.Rating
        }
        shown(ReviewField.Rating)?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Brand.danger) }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            draft.quote,
            { text ->
                vm.edit { it.copy(quote = text.take(REVIEW_QUOTE_MAX)) }
                touched = touched + ReviewField.Quote + ReviewField.Rating
            },
            Modifier.fillMaxWidth().height(140.dp),
            label = { Text("Your review *") },
            placeholder = { Text("Share your experience learning with Darse Quran Academy…") },
            isError = shown(ReviewField.Quote) != null,
            supportingText = { Text(shown(ReviewField.Quote) ?: "${draft.quote.length}/$REVIEW_QUOTE_MAX") },
        )
        OptionalField(draft.course, "Course (optional)", "e.g. Tajweed, Hifz") { text ->
            vm.edit { it.copy(course = text) }
        }
        Spacer(Modifier.height(8.dp))
        OptionalField(draft.location, "Location (optional)", "e.g. Srinagar, J&K") { text ->
            vm.edit { it.copy(location = text) }
        }
        Spacer(Modifier.height(10.dp))
        Banner(
            "Your review will be sent to the admin for verification. If approved, it may appear on the homepage " +
                "(up to $HOMEPAGE_FEATURED_REVIEWS_MAX student reviews are shown at a time).",
            Tone.Warning,
        )
        Spacer(Modifier.height(10.dp))
        message?.let { (t, ok) -> Banner(t, if (ok) Tone.Success else Tone.Danger); Spacer(Modifier.height(8.dp)) }
        PrimaryButton("Submit review", { vm.send() }, enabled = errors.isEmpty(), loading = sending)
    }
}

@Composable
private fun StarPicker(rating: Int, onPick: (Int) -> Unit) {
    Row {
        for (i in 1..REVIEW_RATING_MAX) {
            IconButton({ onPick(i) }) {
                val star = if (i <= rating) Icons.Rounded.Star else Icons.Rounded.StarBorder
                Icon(star, "$i stars", tint = Brand.goldSoft)
            }
        }
    }
}

@Composable
private fun OptionalField(value: String, label: String, placeholder: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value,
        { onChange(it.take(REVIEW_FIELD_MAX)) },
        Modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = true,
    )
}

@Composable
private fun ReviewCard(r: Review) {
    val (label, tone) = reviewStatusLabel(r)
    AppCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f)) {
                repeat(r.rating) { Icon(Icons.Rounded.Star, null, Modifier.size(16.dp), tint = Brand.goldSoft) }
            }
            StatusChip(label, tone)
        }
        Spacer(Modifier.height(6.dp))
        Text("“${r.quote}”", style = MaterialTheme.typography.bodyMedium)
        reviewMeta(r)?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            "Submitted ${formatDate(r.createdAt)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
