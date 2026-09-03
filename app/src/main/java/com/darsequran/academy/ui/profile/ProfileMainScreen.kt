package com.darsequran.academy.ui.profile

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.darsequran.academy.data.local.TokenManager
import com.darsequran.academy.ui.payments.PaymentsViewModel
import com.darsequran.academy.ui.reviews.ReviewsViewModel

@Composable
fun ProfileMainScreen(
    profileViewModel: ProfileViewModel,
    paymentsViewModel: PaymentsViewModel,
    reviewsViewModel: ReviewsViewModel,
    tokenManager: TokenManager,
    onLogout: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        ProfileScreen(
            viewModel = profileViewModel,
            tokenManager = tokenManager,
            onLogout = onLogout
        )
    }
}
