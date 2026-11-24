package com.darsequran.academy.data.repo

import com.darsequran.academy.data.ApiResult
import com.darsequran.academy.data.remote.CartItem
import com.darsequran.academy.data.remote.CartLine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger

/** The signed-in student's server cart, shared by every bookstore screen (and the badge). */
class CartStore(private val repo: AcademyRepository) {
    private val _items = MutableStateFlow<List<CartItem>>(emptyList())
    val items: StateFlow<List<CartItem>> = _items.asStateFlow()

    /** What the server last confirmed; shown again if a change fails. */
    @Volatile private var confirmed: List<CartItem> = emptyList()
    private val sending = Mutex()
    private val pending = AtomicInteger()
    /** The quantity each book should end up at, after every tap so far (guarded by `this`). */
    private val wishes = HashMap<String, Int>()
    /** What was last sent for each book in the current burst of taps (used only while holding [sending]). */
    private val sent = HashMap<String, Int>()

    suspend fun refresh(): ApiResult<List<CartItem>> = repo.cart().also {
        if (it is ApiResult.Ok) { confirmed = it.value; if (pending.get() == 0) _items.value = it.value }
    }

    /**
     * Adds [delta] copies of a book (negative removes; reaching 0 takes the line out), capped at [MAX_PER_BOOK].
     * Returns an error message, or null on success.
     *
     * Quick taps each count: the new quantity builds on every earlier tap and shows at once. Updates go to the
     * server one at a time, each sending the newest quantity wanted (so a burst of taps may need fewer requests,
     * and the cart always ends on the last tap). The server's answer is shown once no tap is waiting; a failure
     * goes back to what the server last confirmed.
     */
    suspend fun change(bookId: String, delta: Int): String? {
        synchronized(this) {
            val now = wishes[bookId] ?: quantityOf(bookId)
            val target = (now + delta).coerceIn(0, MAX_PER_BOOK)
            if (target == now) return null
            wishes[bookId] = target
            pending.incrementAndGet()
            _items.update { lines -> lines.withQuantity(bookId, target) }
        }
        return sending.withLock {
            try {
                val want = synchronized(this) { wishes[bookId] } ?: return@withLock null // an earlier failure reset it
                if (sent[bookId] == want) return@withLock null
                when (val r = repo.setCartLine(bookId, want)) {
                    is ApiResult.Ok -> { sent[bookId] = want; confirmed = r.value; null }
                    is ApiResult.Err -> { synchronized(this) { wishes.remove(bookId) }; _items.value = confirmed; r.message }
                }
            } finally {
                if (pending.decrementAndGet() == 0) {
                    synchronized(this) { wishes.clear() }
                    sent.clear()
                    _items.value = confirmed
                }
            }
        }
    }

    private val _unselected = MutableStateFlow<Set<String>>(emptySet())

    /** Books left out of the next checkout (the website's cart checkboxes); every book starts selected. */
    val unselected: StateFlow<Set<String>> = _unselected.asStateFlow()

    fun select(bookId: String, selected: Boolean) = _unselected.update { if (selected) it - bookId else it + bookId }

    /** "Select all" / "Deselect all". */
    fun selectAll(selected: Boolean) {
        _unselected.value = if (selected) emptySet() else _items.value.map { it.bookId }.toSet()
    }

    /** The cart lines that go to checkout. */
    fun selectedItems(): List<CartItem> = _items.value.filterNot { it.bookId in _unselected.value }

    /** The cart lines left out of checkout. */
    fun unselectedItems(): List<CartItem> = _items.value.filter { it.bookId in _unselected.value }

    /**
     * After an order: the server empties the whole cart, so the books that were left out of it ([left]) are put
     * back, as the website keeps them in the cart. Everything is selected again, as on a fresh cart page.
     */
    suspend fun afterOrder(left: List<CartLine>) {
        sending.withLock { left.forEach { repo.setCartLine(it.bookId, it.quantity) } }
        _unselected.value = emptySet()
        refresh()
    }

    fun quantityOf(bookId: String) = _items.value.firstOrNull { it.bookId == bookId }?.quantity ?: 0

    fun clearLocal() {
        synchronized(this) { wishes.clear() }
        confirmed = emptyList()
        _items.value = emptyList()
        _unselected.value = emptySet()
    }

    /** A book not in the cart yet appears once the server answers (it sends the title, price and cover). */
    private fun List<CartItem>.withQuantity(bookId: String, quantity: Int) =
        if (quantity == 0) filterNot { it.bookId == bookId } else map { if (it.bookId == bookId) it.copy(quantity = quantity) else it }

    companion object {
        const val MAX_PER_BOOK = 20
    }
}
