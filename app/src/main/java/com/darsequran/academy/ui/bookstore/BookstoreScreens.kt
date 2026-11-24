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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.darsequran.academy.ui.common.StatusKind
import com.darsequran.academy.ui.common.statusLabel
import com.darsequran.academy.ui.common.toLoad
import com.darsequran.academy.ui.common.vmFactory
import com.darsequran.academy.ui.common.SearchField
import com.darsequran.academy.ui.common.LoaderViewModel
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
    // Signing in and out is handled by AppContainer; opening a bookstore screen just catches up with changes made
    // elsewhere (e.g. on the website).
    init { if (auth.session.value != null) viewModelScope.launch { cart.refresh() } }
    fun change(bookId: String, delta: Int) = viewModelScope.launch { message.value = cart.change(bookId, delta) }

    /** Whether the account's e-mail is verified (null until known): the website's cart blocks checkout without. */
    val emailVerified = MutableStateFlow<Boolean?>(null)

    fun checkEmail() = viewModelScope.launch {
        (auth.profile() as? ApiResult.Ok)?.let { emailVerified.value = it.value.emailVerified != null }
    }
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
    val vm: LoaderViewModel<Book> = viewModel(key = "book-$id", factory = vmFactory { c -> LoaderViewModel { c.repo.book(id) } })
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
                        !available -> PrimaryButton(
                            bookStatusLabel(book.status), {}, Modifier.width(180.dp), enabled = false,
                        )
                        session == null -> PrimaryButton("Sign in to buy", onSignIn, Modifier.width(180.dp))
                        inCart > 0 -> CartStepper(book.title, inCart, { cartVm.change(id, it) }, Modifier.width(180.dp))
                        else -> PrimaryButton("Add to cart", { cartVm.change(id, 1) }, Modifier.width(180.dp))
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
                    val tone = when (book.status) {
                        "AVAILABLE" -> Tone.Success
                        "COMING_SOON" -> Tone.Info
                        else -> Tone.Danger
                    }
                    StatusChip(bookStatusLabel(book.status), tone)
                    message?.let { Spacer(Modifier.height(10.dp)); Banner(it, Tone.Danger) }
                    Spacer(Modifier.height(16.dp))
                    Text(book.description, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
fun CartStepper(title: String, quantity: Int, onChange: (delta: Int) -> Unit, modifier: Modifier = Modifier) {
    var confirmRemove by remember { mutableStateOf(false) }
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            // Taking the last copy out removes the book from the cart, so ask first.
            IconButton({ if (quantity == 1) confirmRemove = true else onChange(-1) }) { Icon(Icons.Rounded.Remove, "Fewer") }
            Text("$quantity", style = MaterialTheme.typography.titleMedium)
            IconButton({ onChange(1) }, enabled = quantity < CartStore.MAX_PER_BOOK) { Icon(Icons.Rounded.Add, "More") }
        }
    }
    if (confirmRemove) AlertDialog(
        onDismissRequest = { confirmRemove = false },
        title = { Text("Remove from cart?") },
        text = { Text("\"$title\" will be taken out of your cart.") },
        confirmButton = { TextButton({ confirmRemove = false; onChange(-1) }) { Text("Remove") } },
        dismissButton = { TextButton({ confirmRemove = false }) { Text("Keep") } },
    )
}

// ================================================================== cart

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
    /** The lines picked on the cart page. */
    private val lines get() = cart.selectedItems().map { CartLine(it.bookId, it.quantity) }
    /** The lines the shown total was worked out for: the order is placed for exactly these. */
    private var quotedLines: List<CartLine> = emptyList()
    private var quotedTotal: Int = 0

    init { load() }

    fun load() = viewModelScope.launch {
        _state.update { it.copy(quote = Load.Loading) }
        val settings = async { repo.paymentSettings() }
        val s = settings.await()
        val asked = lines
        val quote = repo.checkoutQuote(asked)
        if (quote is ApiResult.Ok) { quotedLines = asked; quotedTotal = quote.value.totalAmountInrPaise }
        _state.update { it.copy(quote = quote.toLoad(), settings = s.orNull(), settingsError = (s as? ApiResult.Err)?.message) }
    }

    /** Fetches the academy's UPI and bank details again (after they failed to load). */
    fun reloadSettings() = viewModelScope.launch {
        _state.update { it.copy(settingsError = null) }
        val r = repo.paymentSettings()
        _state.update { it.copy(settings = r.orNull() ?: it.settings, settingsError = (r as? ApiResult.Err)?.message) }
    }

    /** One order per checkout: a second tap (even in the same frame) or a tap after success is ignored. */
    fun place(
        method: String,
        reference: String,
        address: String,
        pin: String,
        phone: String,
        screenshot: Pair<ByteArray, String>?,
    ) {
        if (_state.value.submitting || _state.value.done) return
        // The student paid the total shown for the quoted cart; if the cart has changed since (another screen, a
        // refresh), placing it would order something other than what was paid for.
        if (lines.toSet() != quotedLines.toSet()) {
            _state.update { it.copy(error = "Your cart changed since this total was worked out. Check the new total below before paying.") }
            load()
            return
        }
        _state.update { it.copy(submitting = true, error = null) }
        val left = cart.unselectedItems().map { CartLine(it.bookId, it.quantity) }
        viewModelScope.launch { send(method, reference, address, pin, phone, screenshot, left) }
    }

    private suspend fun send(
        method: String,
        reference: String,
        address: String,
        pin: String,
        phone: String,
        screenshot: Pair<ByteArray, String>?,
        left: List<CartLine>,
    ) {
        val fields = mapOf(
            "items" to kotlinx.serialization.json.Json.encodeToString(kotlinx.serialization.builtins.ListSerializer(CartLine.serializer()), quotedLines),
            "paymentMethod" to method,
            "upiTransactionId" to reference.trim(),
            "deliveryAddress" to address.trim(),
            "deliveryPinCode" to pin.trim(),
            "deliveryPhoneNumber" to phone.trim(),
            // The total shown; the server refuses the order if it has changed since.
            "expectedTotalPaise" to quotedTotal.toString(),
        )
        when (val r = repo.placeOrder(fields, screenshot)) {
            is ApiResult.Ok -> { cart.afterOrder(left); _state.update { it.copy(submitting = false, done = true) } }
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
    val proof = rememberProofImagePicker()

    Scaffold(topBar = { BackTopBar("Checkout", onBack) }) { padding ->
        Box(Modifier.padding(padding)) {
            if (state.done) {
                MessageState(
                    Icons.Rounded.CheckCircle, "Order submitted",
                    "The academy will verify your payment and ship the books. Track it under Account → Orders.",
                    "Done", onDone,
                )
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
                    DeliveryFields(address, { address = it }, pin, { pin = it }, phone, { phone = it })

                    Text("Payment", style = MaterialTheme.typography.titleLarge)
                    PaymentDetailsError(state.settingsError, vm::reloadSettings)
                    PayWithUpiButton(state.settings, quote.totalAmountInrPaise, "Book order")
                    PaymentDetailsCard(state.settings)
                    PaymentConfirmFields(state.settings, method, { method = it }, reference, { reference = it }, proof)
                    state.error?.let { Banner(it, Tone.Danger) }
                    PrimaryButton(
                        "Submit Order for Approval",
                        { vm.place(method, reference, address, pin, phone, proof.image?.upload) },
                        enabled = address.trim().length >= 10 && pin.length >= 5 && phone.length >= 10 && reference.length >= 6,
                        loading = state.submitting,
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

/** The website's delivery fields (Delivery Address, Pin Code, Phone Number); digits only where it takes digits. */
@Suppress("FunctionNaming") // Compose names composables like types
@Composable
private fun DeliveryFields(
    address: String,
    onAddress: (String) -> Unit,
    pin: String,
    onPin: (String) -> Unit,
    phone: String,
    onPhone: (String) -> Unit,
) {
    OutlinedTextField(
        address, { onAddress(it.take(500)) }, Modifier.fillMaxWidth().height(110.dp),
        label = { Text("Delivery Address") },
        placeholder = { Text("Enter your full address (Street, City, State)...") },
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            pin, { onPin(it.filter(Char::isDigit).take(10)) }, Modifier.weight(1f),
            label = { Text("Pin Code") }, placeholder = { Text("e.g. 193402") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        OutlinedTextField(
            phone, { onPhone(it.filter(Char::isDigit).take(15)) }, Modifier.weight(1.3f),
            label = { Text("Phone Number") }, placeholder = { Text("e.g. 9876543210") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        )
    }
}

// ================================================================ orders

@Composable
fun OrdersScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val vm: LoaderViewModel<List<BookOrder>> = viewModel(key = "orders", factory = vmFactory { c -> LoaderViewModel { c.repo.orders() } })
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
    val (label, tone) = statusLabel(o.status, StatusKind.Order)
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
fun OrderScreen(id: String, onBack: () -> Unit) {
    val vm: LoaderViewModel<BookOrder> = viewModel(key = "order-$id", factory = vmFactory { c -> LoaderViewModel { c.repo.order(id) } })
    val state by vm.state.collectAsStateWithLifecycle()
    Scaffold(topBar = { BackTopBar("Order #${id.takeLast(6).uppercase()}", onBack) }) { padding ->
        Box(Modifier.padding(padding)) {
            LoadContent(state, vm::load) { o ->
                Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    val (label, tone) = statusLabel(o.status, StatusKind.Order)
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
                }
            }
        }
    }
}
