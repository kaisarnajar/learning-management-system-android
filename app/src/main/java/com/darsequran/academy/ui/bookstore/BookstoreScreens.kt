package com.darsequran.academy.ui.bookstore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.darsequran.academy.data.ApiResult
import com.darsequran.academy.data.repo.orNull
import com.darsequran.academy.data.Session
import com.darsequran.academy.data.remote.Book
import com.darsequran.academy.data.remote.BookOrder
import com.darsequran.academy.data.remote.CartLine
import com.darsequran.academy.data.remote.CheckoutQuote
import com.darsequran.academy.data.remote.PaymentSettings
import com.darsequran.academy.data.repo.AcademyRepository
import com.darsequran.academy.data.repo.AuthRepository
import com.darsequran.academy.data.repo.CartStore
import com.darsequran.academy.ui.common.AppCard
import com.darsequran.academy.ui.common.BackTopBar
import com.darsequran.academy.ui.common.Banner
import com.darsequran.academy.ui.common.EmptyState
import com.darsequran.academy.ui.common.ErrorState
import com.darsequran.academy.ui.common.Load
import com.darsequran.academy.ui.common.LoadContent
import com.darsequran.academy.ui.common.LoadingBox
import com.darsequran.academy.ui.common.MessageState
import com.darsequran.academy.ui.common.Overline
import com.darsequran.academy.ui.common.PagedState
import com.darsequran.academy.ui.common.PagedViewModel
import com.darsequran.academy.ui.common.distinctByKey
import com.darsequran.academy.ui.common.rememberProofImagePicker
import com.darsequran.academy.ui.common.LoadMoreEffect
import com.darsequran.academy.ui.common.LoadMoreError
import com.darsequran.academy.ui.common.PriceLine
import com.darsequran.academy.ui.common.PrimaryButton
import com.darsequran.academy.ui.common.RemoteImage
import com.darsequran.academy.ui.common.SecondaryButton
import com.darsequran.academy.ui.common.StatusChip
import com.darsequran.academy.ui.common.Tone
import com.darsequran.academy.ui.common.formatDate
import com.darsequran.academy.ui.common.rupees
import com.darsequran.academy.ui.common.statusLabel
import com.darsequran.academy.ui.common.toLoad
import com.darsequran.academy.ui.common.vmFactory
import com.darsequran.academy.ui.courses.SearchField
import com.darsequran.academy.ui.explore.SimpleLoadViewModel
import com.darsequran.academy.ui.home.BookTile
import com.darsequran.academy.ui.pay.PayWithUpiButton
import com.darsequran.academy.ui.pay.PaymentConfirmFields
import com.darsequran.academy.ui.pay.PaymentDetailsCard
import com.darsequran.academy.ui.pay.PaymentDetailsError
import com.darsequran.academy.ui.theme.Brand
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Cart icon with a count badge, for bookstore top bars. */
@Composable
fun CartAction(cart: CartStore, onClick: () -> Unit) {
    val items by cart.items.collectAsStateWithLifecycle()
    val count = items.sumOf { it.quantity }
    IconButton(onClick) {
        BadgedBox(badge = { if (count > 0) Badge { Text("$count") } }) { Icon(Icons.Rounded.ShoppingCart, "Cart") }
    }
}

class CartViewModel(val cart: CartStore, private val auth: AuthRepository) : ViewModel() {
    val session: StateFlow<Session?> get() = auth.session
    val message = MutableStateFlow<String?>(null)
    init { viewModelScope.launch { auth.session.collect { if (it != null) cart.refresh() else cart.clearLocal() } } }
    fun set(bookId: String, qty: Int) = viewModelScope.launch { message.value = cart.set(bookId, qty) }
}

// ================================================================== list

@Composable
fun BookstoreScreen(onBack: () -> Unit, onOpenBook: (String) -> Unit, onCart: () -> Unit) {
    val vm: PagedViewModel<Book> = viewModel(key = "books", factory = vmFactory { c -> PagedViewModel { p, q, _ -> c.repo.books(p, q) } })
    val cartVm: CartViewModel = viewModel(factory = vmFactory { CartViewModel(it.cart, it.auth) })
    val state by vm.state.collectAsStateWithLifecycle()
    Scaffold(topBar = { BackTopBar("Bookstore", onBack, actions = { CartAction(cartVm.cart, onCart) }) }) { padding ->
        BookGrid(state, vm, Modifier.padding(padding), onOpenBook)
    }
}

@Composable
private fun BookGrid(state: PagedState<Book>, vm: PagedViewModel<Book>, modifier: Modifier, onOpenBook: (String) -> Unit) {
    val grid = rememberLazyGridState()
    val nearEnd by remember { derivedStateOf { (grid.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= grid.layoutInfo.totalItemsCount - 4 } }
    LoadMoreEffect(nearEnd, state, vm)
    PullToRefreshBox(state.refreshing, vm::refresh, modifier.fillMaxSize()) {
        LazyVerticalGrid(
            GridCells.Adaptive(150.dp), state = grid, contentPadding = PaddingValues(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) { SearchField(state.query, "Search books", vm::search) }
            if (state.error != null && state.items.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) { ErrorState(state.error, vm::refresh) }
            else if (!state.loading && state.items.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) { EmptyState("No books found") }
            items(state.items.distinctByKey { it.id }, key = { it.id }) { BookTile(it, Modifier.fillMaxWidth()) { onOpenBook(it.id) } }
            if (state.error != null && state.items.isNotEmpty()) item(span = { GridItemSpan(maxLineSpan) }) { LoadMoreError(state.error, vm::loadMore) }
            if (state.loading && !state.refreshing) item(span = { GridItemSpan(maxLineSpan) }) { LoadingBox(Modifier.fillMaxWidth().height(80.dp)) }
        }
    }
}

// ================================================================ detail

@Composable
fun BookScreen(id: String, onBack: () -> Unit, onCart: () -> Unit, onSignIn: () -> Unit) {
    val vm: SimpleLoadViewModel<Book> = viewModel(key = "book-$id", factory = vmFactory { c -> SimpleLoadViewModel { c.repo.book(id) } })
    val cartVm: CartViewModel = viewModel(factory = vmFactory { CartViewModel(it.cart, it.auth) })
    val state by vm.state.collectAsStateWithLifecycle()
    val session by cartVm.session.collectAsStateWithLifecycle()
    val items by cartVm.cart.items.collectAsStateWithLifecycle()
    val message by cartVm.message.collectAsStateWithLifecycle()
    val inCart = items.firstOrNull { it.bookId == id }?.quantity ?: 0

    Scaffold(
        topBar = { BackTopBar("", onBack, actions = { CartAction(cartVm.cart, onCart) }) },
        bottomBar = {
            val book = (state as? Load.Ready)?.value ?: return@Scaffold
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shadowElevation = 8.dp) {
                Row(Modifier.windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { PriceLine(book.priceInrPaise, book.mrpInrPaise) }
                    val available = book.status == "AVAILABLE"
                    when {
                        !available -> PrimaryButton(if (book.status == "COMING_SOON") "Coming soon" else "Out of stock", {}, Modifier.width(180.dp), enabled = false)
                        session == null -> PrimaryButton("Sign in to buy", onSignIn, Modifier.width(180.dp))
                        inCart > 0 -> QuantityStepper(inCart, { cartVm.set(id, it) }, Modifier.width(180.dp))
                        else -> PrimaryButton("Add to cart", { cartVm.set(id, 1) }, Modifier.width(180.dp))
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            LoadContent(state, vm::load) { book ->
                Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp)) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        RemoteImage(book.imagePath, book.title, Modifier.width(220.dp).aspectRatio(0.72f).clip(MaterialTheme.shapes.large))
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(book.title, style = MaterialTheme.typography.headlineSmall)
                    Text(book.author, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    val (label, tone) = when (book.status) { "AVAILABLE" -> "In stock" to Tone.Success; "COMING_SOON" -> "Coming soon" to Tone.Info; else -> "Out of stock" to Tone.Danger }
                    StatusChip(label, tone)
                    message?.let { Spacer(Modifier.height(10.dp)); Banner(it, Tone.Danger) }
                    Spacer(Modifier.height(16.dp))
                    Text(book.description, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
fun QuantityStepper(quantity: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton({ onChange(quantity - 1) }) { Icon(Icons.Rounded.Remove, "Fewer") }
            Text("$quantity", style = MaterialTheme.typography.titleMedium)
            IconButton({ onChange(quantity + 1) }, enabled = quantity < 20) { Icon(Icons.Rounded.Add, "More") }
        }
    }
}

// ================================================================== cart

@Composable
fun CartScreen(onBack: () -> Unit, onCheckout: () -> Unit, onBrowse: () -> Unit) {
    val vm: CartViewModel = viewModel(factory = vmFactory { CartViewModel(it.cart, it.auth) })
    val items by vm.cart.items.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { BackTopBar("Your cart", onBack) },
        bottomBar = {
            if (items.isNotEmpty()) Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shadowElevation = 8.dp) {
                Column(Modifier.windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Row { Text("Books", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge); Text(rupees(items.sumOf { it.priceInrPaise * it.quantity }), style = MaterialTheme.typography.titleMedium) }
                    Text("Shipping is added at checkout.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    PrimaryButton("Checkout", onCheckout)
                }
            }
        },
    ) { padding ->
        if (items.isEmpty()) Box(Modifier.padding(padding)) { EmptyState("Your cart is empty", "Books you add appear here.", "Browse books", onBrowse) }
        else LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            message?.let { item { Banner(it, Tone.Danger) } }
            items(items, key = { it.bookId }) { item ->
                AppCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RemoteImage(item.imagePath, item.title, Modifier.size(width = 58.dp, height = 80.dp).clip(MaterialTheme.shapes.small))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(item.author, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            PriceLine(item.priceInrPaise, item.mrpInrPaise)
                        }
                        QuantityStepper(item.quantity, { vm.set(item.bookId, it) }, Modifier.width(130.dp))
                    }
                }
            }
        }
    }
}

// ============================================================== checkout

data class CheckoutState(
    val quote: Load<CheckoutQuote> = Load.Loading,
    val settings: PaymentSettings? = null,
    val settingsError: String? = null,
    val submitting: Boolean = false,
    val error: String? = null,
    val done: Boolean = false,
)

class CheckoutViewModel(private val repo: AcademyRepository, private val cart: CartStore) : ViewModel() {
    private val _state = MutableStateFlow(CheckoutState())
    val state: StateFlow<CheckoutState> = _state.asStateFlow()
    private val lines get() = cart.items.value.map { CartLine(it.bookId, it.quantity) }

    init { load() }

    fun load() = viewModelScope.launch {
        _state.update { it.copy(quote = Load.Loading) }
        val settings = async { repo.paymentSettings() }
        val s = settings.await()
        _state.update { it.copy(quote = repo.checkoutQuote(lines).toLoad(), settings = s.orNull(), settingsError = (s as? ApiResult.Err)?.message) }
    }

    /** Fetches the academy's UPI and bank details again (after they failed to load). */
    fun reloadSettings() = viewModelScope.launch {
        _state.update { it.copy(settingsError = null) }
        val r = repo.paymentSettings()
        _state.update { it.copy(settings = r.orNull() ?: it.settings, settingsError = (r as? ApiResult.Err)?.message) }
    }

    /** One order per checkout: a second tap (even in the same frame) or a tap after success is ignored. */
    fun place(method: String, reference: String, address: String, pin: String, phone: String, notes: String, screenshot: Pair<ByteArray, String>?) {
        if (_state.value.submitting || _state.value.done) return
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch { send(method, reference, address, pin, phone, notes, screenshot) }
    }

    private suspend fun send(method: String, reference: String, address: String, pin: String, phone: String, notes: String, screenshot: Pair<ByteArray, String>?) {
        val fields = mapOf(
            "items" to kotlinx.serialization.json.Json.encodeToString(kotlinx.serialization.builtins.ListSerializer(CartLine.serializer()), lines),
            "paymentMethod" to method,
            "upiTransactionId" to reference.trim(),
            "deliveryAddress" to address.trim(),
            "deliveryPinCode" to pin.trim(),
            "deliveryPhoneNumber" to phone.trim(),
            "notes" to notes.trim(),
        )
        when (val r = repo.placeOrder(fields, screenshot)) {
            is ApiResult.Ok -> { cart.refresh(); _state.update { it.copy(submitting = false, done = true) } }
            is ApiResult.Err -> _state.update { it.copy(submitting = false, error = r.message) }
        }
    }
}

@Composable
fun CheckoutScreen(onBack: () -> Unit, onDone: () -> Unit) {
    val vm: CheckoutViewModel = viewModel(factory = vmFactory { CheckoutViewModel(it.repo, it.cart) })
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var method by rememberSaveable { mutableStateOf("upi") }
    var reference by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var pin by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    val proof = rememberProofImagePicker()

    Scaffold(topBar = { BackTopBar("Checkout", onBack) }) { padding ->
        Box(Modifier.padding(padding)) {
            if (state.done) {
                MessageState(Icons.Rounded.CheckCircle, "Order placed", "The academy will verify your payment and ship the books. Track it under Account → Orders.", "Done", onDone)
                return@Box
            }
            LoadContent(state.quote, vm::load) { quote ->
                Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    AppCard(Modifier.fillMaxWidth()) {
                        Overline("Order summary")
                        Spacer(Modifier.height(8.dp))
                        quote.lines.forEach { l ->
                            Row(Modifier.padding(vertical = 3.dp)) {
                                Text("${l.quantity} × ${l.title}", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(rupees(l.lineTotalInrPaise), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Brand.outlineSoft)
                        Row { Text("Shipping", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium); Text(rupees(quote.shippingChargeInrPaise)) }
                        Row(Modifier.padding(top = 6.dp)) {
                            Text("Total", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                            Text(rupees(quote.totalAmountInrPaise), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Text("Delivery", style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(address, { address = it.take(500) }, Modifier.fillMaxWidth().height(110.dp), label = { Text("Full address") })
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(10) }, Modifier.weight(1f), label = { Text("PIN code") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(phone, { phone = it.filter(Char::isDigit).take(15) }, Modifier.weight(1.3f), label = { Text("Phone") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                    }
                    OutlinedTextField(notes, { notes = it.take(300) }, Modifier.fillMaxWidth(), label = { Text("Note for the academy (optional)") })

                    Text("Payment", style = MaterialTheme.typography.titleLarge)
                    PaymentDetailsError(state.settingsError, vm::reloadSettings)
                    PayWithUpiButton(state.settings, quote.totalAmountInrPaise, "Book order")
                    PaymentDetailsCard(state.settings)
                    PaymentConfirmFields(state.settings, method, { method = it }, reference, { reference = it }, proof)
                    state.error?.let { Banner(it, Tone.Danger) }
                    PrimaryButton(
                        "Place order · ${rupees(quote.totalAmountInrPaise)}",
                        { vm.place(method, reference, address, pin, phone, notes, proof.image?.upload) },
                        enabled = address.trim().length >= 10 && pin.length >= 5 && phone.length >= 10 && reference.length >= 6,
                        loading = state.submitting,
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

// ================================================================ orders

@Composable
fun OrdersScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val vm: SimpleLoadViewModel<List<BookOrder>> = viewModel(key = "orders", factory = vmFactory { c -> SimpleLoadViewModel { c.repo.orders() } })
    val state by vm.state.collectAsStateWithLifecycle()
    Scaffold(topBar = { BackTopBar("Book orders", onBack) }) { padding ->
        Box(Modifier.padding(padding)) {
            LoadContent(state, vm::load) { orders ->
                if (orders.isEmpty()) EmptyState("No orders yet", "Books you order appear here with their delivery status.")
                else LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(orders, key = { it.id }) { o -> OrderCard(o) { onOpen(o.id) } }
                }
            }
        }
    }
}

@Composable
private fun OrderCard(o: BookOrder, onClick: () -> Unit) {
    val (label, tone) = statusLabel(o.status)
    AppCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.ShoppingBag, null, tint = Brand.gold)
            Spacer(Modifier.width(10.dp))
            Text("Order #${o.id.takeLast(6).uppercase()}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            StatusChip(label, tone)
        }
        Spacer(Modifier.height(6.dp))
        Text(o.items.joinToString { "${it.quantity} × ${it.book?.title ?: "Book"}" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(6.dp))
        Row { Text(formatDate(o.createdAt), Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(rupees(o.totalAmountInrPaise), style = MaterialTheme.typography.titleMedium) }
    }
}

@Composable
fun OrderScreen(id: String, onBack: () -> Unit, onReceipt: (String) -> Unit) {
    val vm: SimpleLoadViewModel<BookOrder> = viewModel(key = "order-$id", factory = vmFactory { c -> SimpleLoadViewModel { c.repo.order(id) } })
    val state by vm.state.collectAsStateWithLifecycle()
    Scaffold(topBar = { BackTopBar("Order #${id.takeLast(6).uppercase()}", onBack) }) { padding ->
        Box(Modifier.padding(padding)) {
            LoadContent(state, vm::load) { o ->
                Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    val (label, tone) = statusLabel(o.status)
                    Row(verticalAlignment = Alignment.CenterVertically) { StatusChip(label, tone); Spacer(Modifier.width(10.dp)); Text("Placed ${formatDate(o.createdAt)}", style = MaterialTheme.typography.bodySmall) }
                    if (o.status == "SHIPPED") Banner("Shipped with ${o.courierServiceName ?: "the courier"}${o.trackingId?.let { " · tracking $it" } ?: ""}", Tone.Info, icon = Icons.Rounded.LocalShipping)
                    AppCard(Modifier.fillMaxWidth()) {
                        o.items.forEach { i ->
                            Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                RemoteImage(i.book?.imagePath, i.book?.title, Modifier.size(width = 44.dp, height = 60.dp).clip(MaterialTheme.shapes.small))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) { Text(i.book?.title ?: "Book", style = MaterialTheme.typography.titleSmall); Text("${i.quantity} × ${rupees(i.priceAtPurchaseInrPaise)}", style = MaterialTheme.typography.bodySmall) }
                                Text(rupees(i.quantity * i.priceAtPurchaseInrPaise), style = MaterialTheme.typography.titleSmall)
                            }
                        }
                        HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Brand.outlineSoft)
                        Row { Text("Shipping", Modifier.weight(1f)); Text(rupees(o.shippingChargeInrPaise)) }
                        Row { Text("Total", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium); Text(rupees(o.totalAmountInrPaise), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
                    }
                    AppCard(Modifier.fillMaxWidth()) {
                        Overline("Delivery")
                        Spacer(Modifier.height(6.dp))
                        Text(o.deliveryAddress ?: "", style = MaterialTheme.typography.bodyMedium)
                        Text(listOfNotNull(o.deliveryPinCode, o.deliveryPhoneNumber).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (o.paymentRecordId != null && o.receiptGeneratedAt != null) SecondaryButton("Download receipt", { onReceipt(o.paymentRecordId) })
                }
            }
        }
    }
}
