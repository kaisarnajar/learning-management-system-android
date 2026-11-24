package com.darsequran.academy.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darsequran.academy.data.ApiResult
import com.darsequran.academy.data.remote.Review
import com.darsequran.academy.data.repo.AcademyRepository
import com.darsequran.academy.data.repo.AuthRepository
import com.darsequran.academy.data.repo.orNull
import com.darsequran.academy.ui.common.Load
import com.darsequran.academy.ui.common.toLoad
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class ReviewsViewModel(private val repo: AcademyRepository, private val auth: AuthRepository) : ViewModel() {
    val state = MutableStateFlow<Load<List<Review>>>(Load.Loading)
    val sending = MutableStateFlow(false)
    val message = MutableStateFlow<Pair<String, Boolean>?>(null)
    val draft = MutableStateFlow(ReviewDraft())
    private var address: String? = null
    init { load() }

    fun load() = viewModelScope.launch {
        val first = state.value !is Load.Ready
        val profile = async { if (first) auth.profile().orNull() else null }
        val reviews = repo.myReviews().toLoad()
        if (first) {
            address = profile.await()?.address
            // The location starts as the student's address unless they already typed one.
            if (draft.value.location.isEmpty()) {
                draft.value = draft.value.copy(location = newReviewDraft(address).location)
            }
        }
        state.value = reviews
    }

    fun edit(change: (ReviewDraft) -> ReviewDraft) { draft.value = change(draft.value) }

    fun send() = viewModelScope.launch {
        if (reviewErrors(draft.value).isNotEmpty()) return@launch
        sending.value = true
        when (val r = repo.addReview(reviewRequest(draft.value))) {
            is ApiResult.Ok -> {
                message.value = REVIEW_SENT to true
                draft.value = newReviewDraft(address)
                load()
            }
            is ApiResult.Err -> message.value = r.message to false
        }
        sending.value = false
    }

    companion object {
        const val REVIEW_SENT = "Thank you! Your review was submitted for admin approval."
    }
}
