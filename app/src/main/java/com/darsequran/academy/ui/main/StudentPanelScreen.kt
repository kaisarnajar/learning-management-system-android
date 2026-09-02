package com.darsequran.academy.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.darsequran.academy.data.local.TokenManager
import com.darsequran.academy.data.repository.AuthRepository
import com.darsequran.academy.ui.courses.MyCoursesScreen
import com.darsequran.academy.ui.courses.MyCoursesViewModel
import com.darsequran.academy.ui.notifications.NotificationsScreen
import com.darsequran.academy.ui.notifications.NotificationsViewModel
import com.darsequran.academy.ui.payments.PaymentsScreen
import com.darsequran.academy.ui.payments.PaymentsViewModel
import com.darsequran.academy.ui.profile.ProfileScreen
import com.darsequran.academy.ui.profile.ProfileViewModel
import com.darsequran.academy.ui.reviews.ReviewsScreen
import com.darsequran.academy.ui.reviews.ReviewsViewModel
import com.darsequran.academy.ui.theme.EmeraldDark
import com.darsequran.academy.ui.theme.EmeraldPrimary
import com.darsequran.academy.ui.theme.GoldAccent

@Composable
fun StudentPanelScreen(
    tokenManager: TokenManager,
    authRepository: AuthRepository,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var activeSubView by remember { mutableStateOf("menu") } // "menu" or "reviews"

    val notificationsViewModel: NotificationsViewModel = viewModel(
        factory = NotificationsViewModel.Factory(authRepository)
    )
    val notificationsUiState by notificationsViewModel.uiState.collectAsState()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        bottomBar = {
            NavigationBar(
                containerColor = EmeraldDark,
                contentColor = Color.White
            ) {
                // Tab 0: Profile
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = if (selectedTab == 0) GoldAccent else Color.White.copy(alpha = 0.7f)
                        )
                    },
                    label = {
                        Text(
                            text = "Profile",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 0) GoldAccent else Color.White.copy(alpha = 0.7f)
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = EmeraldPrimary
                    )
                )

                // Tab 1: Notifications
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (notificationsUiState.unreadCount > 0) {
                                    Badge { Text("${notificationsUiState.unreadCount}") }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = if (selectedTab == 1) GoldAccent else Color.White.copy(alpha = 0.7f)
                            )
                        }
                    },
                    label = {
                        Text(
                            text = "Notifications",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) GoldAccent else Color.White.copy(alpha = 0.7f)
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = EmeraldPrimary
                    )
                )

                // Tab 2: My Courses
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = "My Courses",
                            tint = if (selectedTab == 2) GoldAccent else Color.White.copy(alpha = 0.7f)
                        )
                    },
                    label = {
                        Text(
                            text = "My Courses",
                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 2) GoldAccent else Color.White.copy(alpha = 0.7f)
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = EmeraldPrimary
                    )
                )

                // Tab 3: Payments
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = "Payments",
                            tint = if (selectedTab == 3) GoldAccent else Color.White.copy(alpha = 0.7f)
                        )
                    },
                    label = {
                        Text(
                            text = "Payments",
                            fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 3) GoldAccent else Color.White.copy(alpha = 0.7f)
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = EmeraldPrimary
                    )
                )

                // Tab 4: Dashboard / More
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = {
                        selectedTab = 4
                        activeSubView = "menu"
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = "Dashboard",
                            tint = if (selectedTab == 4) GoldAccent else Color.White.copy(alpha = 0.7f)
                        )
                    },
                    label = {
                        Text(
                            text = "Dashboard",
                            fontWeight = if (selectedTab == 4) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 4) GoldAccent else Color.White.copy(alpha = 0.7f)
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = EmeraldPrimary
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> {
                    val profileViewModel: ProfileViewModel = viewModel(
                        factory = ProfileViewModel.Factory(authRepository)
                    )
                    ProfileScreen(viewModel = profileViewModel)
                }
                1 -> {
                    NotificationsScreen(viewModel = notificationsViewModel)
                }
                2 -> {
                    val myCoursesViewModel: MyCoursesViewModel = viewModel(
                        factory = MyCoursesViewModel.Factory(authRepository)
                    )
                    MyCoursesScreen(viewModel = myCoursesViewModel)
                }
                3 -> {
                    val paymentsViewModel: PaymentsViewModel = viewModel(
                        factory = PaymentsViewModel.Factory(authRepository)
                    )
                    PaymentsScreen(viewModel = paymentsViewModel)
                }
                4 -> {
                    if (activeSubView == "reviews") {
                        val reviewsViewModel: ReviewsViewModel = viewModel(
                            factory = ReviewsViewModel.Factory(authRepository)
                        )
                        ReviewsScreen(viewModel = reviewsViewModel)
                    } else {
                        MoreMenuSheet(
                            onSelectReviews = { activeSubView = "reviews" },
                            onSelectCart = { activeSubView = "reviews" },
                            onLogout = onLogout
                        )
                    }
                }
                else -> {
                    val profileViewModel: ProfileViewModel = viewModel(
                        factory = ProfileViewModel.Factory(authRepository)
                    )
                    ProfileScreen(viewModel = profileViewModel)
                }
            }
        }
    }
}
