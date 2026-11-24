package com.darsequran.academy.ui.manage.bookstore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.darsequran.academy.data.staff.PickedFile
import com.darsequran.academy.data.staff.formOf
import com.darsequran.academy.ui.common.Load
import com.darsequran.academy.ui.common.PriceLine
import com.darsequran.academy.ui.common.ReloadOnReturn
import com.darsequran.academy.ui.common.RemoteImage
import com.darsequran.academy.ui.common.StatusChip
import com.darsequran.academy.ui.common.Tone
import com.darsequran.academy.ui.common.formatDate
import com.darsequran.academy.ui.common.rupees
import com.darsequran.academy.ui.manage.common.AddFab
import com.darsequran.academy.ui.manage.common.ConfirmDialog
import com.darsequran.academy.ui.manage.common.DetailCard
import com.darsequran.academy.ui.manage.common.Field
import com.darsequran.academy.ui.manage.common.FieldRow
import com.darsequran.academy.ui.manage.common.FormDropdown
import com.darsequran.academy.ui.manage.common.FormImageField
import com.darsequran.academy.ui.manage.common.FormMoneyField
import com.darsequran.academy.ui.manage.common.FormNumberField
import com.darsequran.academy.ui.manage.common.FormScreen
import com.darsequran.academy.ui.manage.common.FormSection
import com.darsequran.academy.ui.manage.common.FormSwitch
import com.darsequran.academy.ui.manage.common.FormTextField
import com.darsequran.academy.ui.manage.common.LoadedColumn
import com.darsequran.academy.ui.manage.common.ManageListScreen
import com.darsequran.academy.ui.manage.common.ManageScaffold
import com.darsequran.academy.ui.manage.common.RecordCard
import com.darsequran.academy.ui.manage.common.paiseToRupeesText
import com.darsequran.academy.ui.manage.common.rememberActionRunner
import com.darsequran.academy.ui.manage.common.rememberLoader
import com.darsequran.academy.ui.manage.common.rememberPaged
import com.darsequran.academy.ui.manage.common.FeaturedSwitch

/*
 * The book catalogue: /admin/bookstore (list), /admin/bookstore/[bookId] (detail), /new and /[bookId]/edit (form).
 */

@Composable
private fun BookCover(path: String?, title: String, width: Int = 40, height: Int = 54) {
    RemoteImage(path, title, Modifier.size(width.dp, height.dp).clip(MaterialTheme.shapes.small))
}

@Composable
fun BooksListScreen(onBack: () -> Unit, onOpen: (String) -> Unit, onAdd: () -> Unit, onOrders: () -> Unit) {
    val vm = rememberPaged("admin-books") { page, q, _ -> service<BookstoreAdminApi>().books(page, q.ifBlank { null }) }
    ReloadOnReturn(vm::refresh)
    ManageListScreen(
        title = "Bookstore",
        onBack = onBack,
        vm = vm,
        key = { it.id },
        emptyTitle = "No books yet",
        searchHint = "Search by title or author",
        fab = { AddFab("Add book", onAdd) },
        actions = { IconButton(onClick = onOrders) { Icon(Icons.AutoMirrored.Rounded.ReceiptLong, "Book orders") } },
    ) { book ->
        RecordCard(
            title = book.title,
            subtitle = "${book.author} · ${rupees(book.priceInrPaise)}",
            meta = listOf(
                if (book.published) "Published" else "Draft",
                if (book.featured) "Featured on homepage" else "Not featured",
            ).joinToString(" · "),
            chip = book.statusLabel.ifBlank { book.status } to bookStatusTone(book.status),
            leading = { BookCover(book.imagePath, book.title) },
            onClick = { onOpen(book.id) },
        )
    }
}

@Composable
fun BookDetailScreen(id: String, onBack: () -> Unit, onEdit: () -> Unit) {
    val loader = rememberLoader("admin-book-$id") { service<BookstoreAdminApi>().book(id) }
    ReloadOnReturn { loader.reload() }
    val runner = rememberActionRunner()
    var confirmDelete by remember { mutableStateOf(false) }

    ManageScaffold("Book details", onBack, runner.snackbar, actions = {
        IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, "Edit book") }
        IconButton(onClick = { confirmDelete = true }, enabled = !runner.busy) { Icon(Icons.Rounded.Delete, "Delete book") }
    }) { padding ->
        LoadedColumn(loader, padding) { r ->
            val b = r.book
            item {
                DetailCard {
                    Row {
                        BookCover(b.imagePath, b.title, 96, 128)
                        Spacer(Modifier.width(14.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(b.title, style = MaterialTheme.typography.titleLarge)
                            Text(b.author, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            PriceLine(b.priceInrPaise, b.mrpInrPaise.takeIf { it > 0 })
                            StatusChip(b.statusLabel.ifBlank { b.status }, bookStatusTone(b.status))
                            StatusChip(if (b.published) "Published" else "Draft", if (b.published) Tone.Success else Tone.Neutral)
                        }
                    }
                }
            }
            item {
                DetailCard("Details") {
                    Field("Title", b.title)
                    Field("Author", b.author)
                    Field("Selling price", rupees(b.priceInrPaise))
                    Field("MRP", b.mrpInrPaise.takeIf { it > 0 }?.let(::rupees))
                    Field("Purchase price", rupees(b.purchasePriceInrPaise))
                    Field("Weight", "${b.weightInGrams} g")
                    Field("Homepage", if (b.featured) "Featured" else "Not featured")
                    Field("Description", b.description)
                    Field("Added", formatDate(b.createdAt))
                }
            }
            item {
                DetailCard("Stock") {
                    val s = r.stock
                    FieldRow("Total inventory procured", b.inventoryPurchased.toString())
                    if (s != null) {
                        if (s.adjustments != 0) FieldRow("Adjustments", (if (s.adjustments > 0) "+" else "") + s.adjustments)
                        FieldRow("Held by approved or shipped orders", s.committed.toString())
                        FieldRow("Available", s.available.toString(), strong = true)
                    }
                    Text(
                        "To change the stock, edit the total inventory procured. Refunded and declined orders do not hold stock.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (confirmDelete) ConfirmDialog(
        "Delete this book?",
        "The book and its cover are removed. A book that appears in any order cannot be deleted: unpublish it or set it to Out of Stock instead.",
        "Delete", destructive = true,
        onConfirm = { runner.run({ service<BookstoreAdminApi>().delete(id) }, success = "Book deleted.") { onBack() } },
        onDismiss = { confirmDelete = false },
    )
}

private val defaultStatusOptions = listOf("AVAILABLE" to "Available", "OUT_OF_STOCK" to "Out of Stock", "COMING_SOON" to "Coming Soon")

/** Add ([id] null) or edit a book: the website's BookForm. */
@Composable
fun BookFormScreen(id: String?, onBack: () -> Unit) {
    val loader = rememberLoader("admin-book-form-${id ?: "new"}") {
        val api = service<BookstoreAdminApi>()
        if (id == null) api.formOptions().let { AdminBookResponse(AdminBook(""), null, it.statusOptions, it.featuredCount, it.featuredMax) }
        else api.book(id)
    }
    val runner = rememberActionRunner()
    val state by loader.state.collectAsStateWithLifecycle()
    val loaded = (state as? Load.Ready)?.value

    var filled by rememberSaveable { mutableStateOf(false) }
    var title by rememberSaveable { mutableStateOf("") }
    var author by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var mrp by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var purchase by rememberSaveable { mutableStateOf("") }
    var weight by rememberSaveable { mutableStateOf("0") }
    var inventory by rememberSaveable { mutableStateOf("") }
    var status by rememberSaveable { mutableStateOf("AVAILABLE") }
    var published by rememberSaveable { mutableStateOf(true) }
    var featured by rememberSaveable { mutableStateOf(false) }
    var removeImage by rememberSaveable { mutableStateOf(false) }
    var image by remember { mutableStateOf<PickedFile?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(loaded) {
        val b = loaded?.book
        if (b != null && id != null && !filled) {
            title = b.title; author = b.author; description = b.description
            mrp = if (b.mrpInrPaise > 0) paiseToRupeesText(b.mrpInrPaise) else ""
            price = paiseToRupeesText(b.priceInrPaise); purchase = paiseToRupeesText(b.purchasePriceInrPaise)
            weight = b.weightInGrams.toString(); inventory = b.inventoryPurchased.toString()
            status = b.status; published = b.published; featured = b.featuredOnHomepage
            filled = true
        }
    }

    fun validate(): String? = when {
        title.trim().length < 2 -> "Title is required."
        author.trim().length < 2 -> "Author is required."
        description.trim().length < 10 -> "Description must be at least 10 characters."
        price.isBlank() -> "Price is required."
        purchase.isBlank() -> "Purchase price is required."
        weight.isBlank() -> "Weight is required."
        inventory.isBlank() -> "Total inventory procured is required."
        else -> null
    }

    FormScreen(
        title = if (id == null) "Add book" else "Edit book",
        onBack = onBack,
        saving = runner.busy,
        snackbar = runner.snackbar,
        saveLabel = if (id == null) "Add book" else "Save changes",
        error = error,
        canSave = loaded != null && (id == null || filled),
        loadError = (state as? Load.Failed)?.message,
        onRetry = { loader.reload(true) },
        onSave = {
            error = validate()
            if (error == null) {
                val parts = formOf {
                    text("title", title.trim()); text("author", author.trim()); text("description", description.trim())
                    text("mrpInr", mrp); text("priceInr", price); text("purchasePriceInr", purchase)
                    text("weightInGrams", weight); text("inventoryPurchased", inventory); text("status", status)
                    text("published", published.toString()); text("featuredOnHomepage", featured.toString())
                    text("removeImage", (removeImage && image == null).toString())
                    file("image", image)
                }
                runner.run(
                    { if (id == null) service<BookstoreAdminApi>().create(parts) else service<BookstoreAdminApi>().update(id, parts) },
                    success = if (id == null) "Book created." else "Changes saved.",
                ) { onBack() }
            }
        },
    ) {
        if (loaded == null) { Text("Loading…"); return@FormScreen }
        val currentImage = loaded.book.imagePath.takeIf { !removeImage }
        FormImageField(
            "Book cover image", currentImage, image, { image = it },
            supporting = "Optional, JPEG, PNG, WebP or GIF up to 2 MB.",
            onRemoveCurrent = if (id != null) ({ removeImage = true }) else null,
        )
        FormTextField("Title", title, { title = it }, maxLength = 200)
        FormTextField("Author", author, { author = it }, maxLength = 200)
        FormTextField("Description", description, { description = it }, singleLine = false, minLines = 4, maxLength = 5000)

        FormSection("Price and stock") {
            FormMoneyField("MRP (₹)", mrp, { mrp = it }, supporting = "Optional, e.g. 500")
            FormMoneyField("Selling price (₹)", price, { price = it }, supporting = "e.g. 299")
            FormMoneyField("Purchase price (₹)", purchase, { purchase = it }, supporting = "e.g. 150")
            FormNumberField("Weight (grams)", weight, { weight = it }, supporting = "e.g. 150")
            FormNumberField("Total inventory procured", inventory, { inventory = it }, supporting = "e.g. 100")
            FormDropdown(
                "Availability status",
                loaded.statusOptions.map { it.value to it.label }.ifEmpty { defaultStatusOptions },
                status, { status = it },
            )
        }

        FormSection("Visibility") {
            FormSwitch("Published", published, { published = it }, "Turn off to hide from the public bookstore.")
            FeaturedSwitch(featured, { featured = it }, loaded.book.featuredOnHomepage, loaded.featuredCount, loaded.featuredMax, "Featured Books", "bookstore", "books")
        }
        Spacer(Modifier.height(4.dp))
    }
}
