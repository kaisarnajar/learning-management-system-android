package com.darsequran.academy.ui.bookstore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.darsequran.academy.data.remote.CartItem
import com.darsequran.academy.ui.common.AppCard
import com.darsequran.academy.ui.common.BackTopBar
import com.darsequran.academy.ui.common.Banner
import com.darsequran.academy.ui.common.EmptyState
import com.darsequran.academy.ui.common.PriceLine
import com.darsequran.academy.ui.common.PrimaryButton
import com.darsequran.academy.ui.common.RemoteImage
import com.darsequran.academy.ui.common.Tone
import com.darsequran.academy.ui.common.rupees
import com.darsequran.academy.ui.common.vmFactory

/** A book left out of checkout is shown faded, as on the website's cart page. */
private const val UNSELECTED_ALPHA = 0.5f

/** The website's book status words (services/bookstore.ts bookStatusLabel). */
internal fun bookStatusLabel(status: String): String = when (status) {
    "AVAILABLE" -> "Available"
    "OUT_OF_STOCK" -> "Out of Stock"
    "COMING_SOON" -> "Coming Soon"
    else -> status
}

/** "1 item", "3 items", as the website counts the cart. */
internal fun itemCount(count: Int): String = "$count item${if (count != 1) "s" else ""}"

/** What the cart's checkout button offers, as on the website's cart page. */
internal enum class CartCheckout { VerifyEmail, NothingSelected, Ready }

/**
 * The website blocks checkout until the e-mail is verified; [emailVerified] is null while unknown (the server
 * refuses the order anyway).
 */
internal fun cartCheckout(emailVerified: Boolean?, selectedCount: Int): CartCheckout = when {
    emailVerified == false -> CartCheckout.VerifyEmail
    selectedCount == 0 -> CartCheckout.NothingSelected
    else -> CartCheckout.Ready
}

@Suppress("FunctionNaming") // Compose names composables like types
@Composable
fun CartScreen(onBack: () -> Unit, onCheckout: () -> Unit, onBrowse: () -> Unit) {
    val vm: CartViewModel = viewModel(factory = vmFactory { CartViewModel(it.cart, it.auth) })
    val items by vm.cart.items.collectAsStateWithLifecycle()
    val unselected by vm.cart.unselected.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val emailVerified by vm.emailVerified.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.checkEmail() }
    Scaffold(
        topBar = { BackTopBar("My Cart", onBack) },
        bottomBar = {
            if (items.isNotEmpty()) CartSummary(items.filterNot { it.bookId in unselected }, emailVerified, onCheckout)
        },
    ) { padding ->
        if (items.isEmpty()) {
            Box(Modifier.padding(padding)) {
                EmptyState("Your cart is empty", "Browse our bookstore to add books.", "Browse Bookstore", onBrowse)
            }
            return@Scaffold
        }
        LazyColumn(
            Modifier.padding(padding), contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "Select the books you want to buy, then proceed to checkout.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton({ vm.cart.selectAll(true) }) { Text("Select all") }
                    TextButton({ vm.cart.selectAll(false) }) { Text("Deselect all") }
                    Spacer(Modifier.weight(1f))
                    val count = items.sumOf { it.quantity }
                    Text("${itemCount(count)} in cart", style = MaterialTheme.typography.bodySmall)
                }
            }
            message?.let { item { Banner(it, Tone.Danger) } }
            items(items, key = { it.bookId }) { item ->
                val selected = item.bookId !in unselected
                CartRow(item, selected, { vm.cart.select(item.bookId, it) }) { vm.change(item.bookId, it) }
            }
        }
    }
}

@Suppress("FunctionNaming") // Compose names composables like types
@Composable
private fun CartRow(item: CartItem, selected: Boolean, onSelect: (Boolean) -> Unit, onChange: (Int) -> Unit) {
    val faded = Modifier.fillMaxWidth().alpha(if (selected) 1f else UNSELECTED_ALPHA)
    AppCard(faded, contentPadding = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(selected, onSelect, Modifier.semantics { contentDescription = "Select ${item.title}" })
            val cover = Modifier.size(width = 48.dp, height = 66.dp).clip(MaterialTheme.shapes.small)
            RemoteImage(item.imagePath, item.title, cover)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.title, style = MaterialTheme.typography.titleMedium,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    item.author, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PriceLine(item.priceInrPaise, item.mrpInrPaise)
            }
            CartStepper(item.title, item.quantity, onChange, Modifier.width(120.dp))
        }
    }
}

@Suppress("FunctionNaming") // Compose names composables like types
@Composable
private fun CartSummary(selected: List<CartItem>, emailVerified: Boolean?, onCheckout: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shadowElevation = 8.dp) {
        Column(
            Modifier.windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${itemCount(selected.size)} selected", Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "Total: ${rupees(selected.sumOf { it.priceInrPaise * it.quantity })}",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(Modifier.height(10.dp))
            when (cartCheckout(emailVerified, selected.size)) {
                CartCheckout.VerifyEmail -> Banner("Verify your email to checkout", Tone.Warning)
                CartCheckout.NothingSelected -> PrimaryButton("Proceed to Checkout (0 items)", {}, enabled = false)
                CartCheckout.Ready -> PrimaryButton(
                    "Proceed to Checkout (${selected.size} ${if (selected.size == 1) "item" else "items"})", onCheckout,
                )
            }
        }
    }
}
