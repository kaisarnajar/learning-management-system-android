package com.darsequran.academy.data.repo

import com.darsequran.academy.data.ApiResult
import com.darsequran.academy.data.remote.CartItem
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

    suspend fun refresh(): ApiResult<List<CartItem>> = repo.cart().also {
        if (it is ApiResult.Ok) { confirmed = it.value; if (pending.get() == 0) _items.value = it.value }
    }

    /**
     * Adds [delta] copies of a book (negative removes; reaching 0 takes the line out), capped at [MAX_PER_BOOK].
     * Returns an error message, or null on success.
     *
     * Quick taps each count: the new quantity is worked out from what is on screen now (earlier taps included) and
     * shown at once, and the updates go to the server one at a time, in order. The server's answer is shown once no
     * other change is waiting, so an older answer can't undo a newer tap.
     */
    suspend fun change(bookId: String, delta: Int): String? {
        val target = (quantityOf(bookId) + delta).coerceIn(0, MAX_PER_BOOK)
        if (target == quantityOf(bookId)) return null
        pending.incrementAndGet()
        _items.update { lines -> lines.withQuantity(bookId, target) }
        return sending.withLock {
            val r = repo.setCartLine(bookId, target)
            val last = pending.decrementAndGet() == 0
            when (r) {
                is ApiResult.Ok -> { confirmed = r.value; if (last) _items.value = r.value; null }
                is ApiResult.Err -> { _items.value = confirmed; r.message }
            }
        }
    }

    fun quantityOf(bookId: String) = _items.value.firstOrNull { it.bookId == bookId }?.quantity ?: 0

    fun clearLocal() { confirmed = emptyList(); _items.value = emptyList() }

    /** A book not in the cart yet appears once the server answers (it sends the title, price and cover). */
    private fun List<CartItem>.withQuantity(bookId: String, quantity: Int) =
        if (quantity == 0) filterNot { it.bookId == bookId } else map { if (it.bookId == bookId) it.copy(quantity = quantity) else it }

    companion object {
        const val MAX_PER_BOOK = 20
    }
}
