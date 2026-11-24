package com.darsequran.academy.data.repo

import com.darsequran.academy.data.ApiResult
import com.darsequran.academy.data.remote.CartItem
import com.darsequran.academy.data.remote.CartLine
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
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
    /**
     * Bumped by [clearLocal] (sign-out, another account): an answer to a request sent before it belongs to the
     * previous cart and is dropped.
     */
    @Volatile private var generation = 0

    suspend fun refresh(): ApiResult<List<CartItem>> {
        val asked = generation
        return repo.cart().also {
            if (it is ApiResult.Ok && asked == generation) { confirmed = it.value; if (pending.get() == 0) _items.value = it.value }
        }
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
    suspend fun change(bookId: String, delta: Int): String? = update(bookId) { now -> now + delta }

    /**
     * Puts a book in the cart (one copy), or leaves it as it is if it is already there or on its way: a second tap on
     * "Add to cart" before the server answers doesn't add a second copy.
     */
    suspend fun add(bookId: String): String? = update(bookId) { now -> maxOf(now, 1) }

    private suspend fun update(bookId: String, to: (now: Int) -> Int): String? {
        val asked: Int
        synchronized(this) {
            asked = generation
            val now = wishes[bookId] ?: quantityOf(bookId)
            val target = to(now).coerceIn(0, MAX_PER_BOOK)
            if (target == now) return null
            wishes[bookId] = target
            pending.incrementAndGet()
            _items.update { lines -> lines.withQuantity(bookId, target) }
        }
        return sending.withLock {
            try {
                val want = synchronized(this) { wishes[bookId] } ?: return@withLock null // an earlier failure reset it
                if (sent[bookId] == want || asked != generation) return@withLock null
                val r = repo.setCartLine(bookId, want)
                if (asked != generation) return@withLock null // signed out meanwhile: not this cart any more
                when (r) {
                    is ApiResult.Ok -> { sent[bookId] = want; confirmed = r.value; null }
                    is ApiResult.Err -> { synchronized(this) { wishes.remove(bookId) }; _items.value = confirmed; r.message }
                }
            } finally {
                if (pending.decrementAndGet() == 0) {
                    synchronized(this) { wishes.clear() }
                    sent.clear()
                    if (asked == generation) _items.value = confirmed
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
     *
     * Not cancellable: the order is placed, and leaving the checkout screen now must not lose those books. Returns
     * how many of them couldn't be put back (out of stock since, or no connection).
     */
    suspend fun afterOrder(left: List<CartLine>): Int = withContext(NonCancellable) {
        val failed = sending.withLock { left.count { repo.setCartLine(it.bookId, it.quantity) is ApiResult.Err } }
        _unselected.value = emptySet()
        refresh()
        failed
    }

    fun quantityOf(bookId: String) = _items.value.firstOrNull { it.bookId == bookId }?.quantity ?: 0

    fun clearLocal() {
        synchronized(this) { generation++; wishes.clear() }
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
