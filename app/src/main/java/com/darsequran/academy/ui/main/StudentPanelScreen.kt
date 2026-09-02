package com.darsequran.academy.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.darsequran.academy.data.local.TokenManager
import com.darsequran.academy.data.repository.AuthRepository
import com.darsequran.academy.ui.announcements.AnnouncementsViewModel
import com.darsequran.academy.ui.blog.BlogViewModel
import com.darsequran.academy.ui.bookstore.BookstoreViewModel
import com.darsequran.academy.ui.community.CommunityMainScreen
import com.darsequran.academy.ui.courses.CoursesCatalogViewModel
import com.darsequran.academy.ui.courses.CoursesMainScreen
import com.darsequran.academy.ui.courses.MyCoursesViewModel
import com.darsequran.academy.ui.fatwa.FatwaViewModel
import com.darsequran.academy.ui.home.HomeScreen
import com.darsequran.academy.ui.home.HomeViewModel
import com.darsequran.academy.ui.library.DigitalLibraryViewModel
import com.darsequran.academy.ui.library.LibraryMainScreen
import com.darsequran.academy.ui.notifications.NotificationsViewModel
import com.darsequran.academy.ui.payments.PaymentsViewModel
import com.darsequran.academy.ui.profile.ProfileMainScreen
import com.darsequran.academy.ui.profile.ProfileViewModel
import com.darsequran.academy.ui.reviews.ReviewsViewModel
import com.darsequran.academy.ui.teachers.TeachersViewModel
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

    val notificationsViewModel: NotificationsViewModel = viewModel(
        factory = NotificationsViewModel.Factory(authRepository)
    )
    val notificationsUiState by notificationsViewModel.uiState.collectAsState()

    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(authRepository)
    )

    val myCoursesViewModel: MyCoursesViewModel = viewModel(
        factory = MyCoursesViewModel.Factory(authRepository)
    )
    val coursesCatalogViewModel: CoursesCatalogViewModel = viewModel(
        factory = CoursesCatalogViewModel.Factory(authRepository)
    )

    val libraryViewModel: DigitalLibraryViewModel = viewModel(
        factory = DigitalLibraryViewModel.Factory(authRepository)
    )
    val bookstoreViewModel: BookstoreViewModel = viewModel(
        factory = BookstoreViewModel.Factory(authRepository)
    )

    val announcementsViewModel: AnnouncementsViewModel = viewModel(
        factory = AnnouncementsViewModel.Factory(authRepository)
    )
    val blogViewModel: BlogViewModel = viewModel(
        factory = BlogViewModel.Factory(authRepository)
    )
    val fatwaViewModel: FatwaViewModel = viewModel(
        factory = FatwaViewModel.Factory(authRepository)
    )

    val profileViewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.Factory(authRepository)
    )
    val paymentsViewModel: PaymentsViewModel = viewModel(
        factory = PaymentsViewModel.Factory(authRepository)
    )
    val teachersViewModel: TeachersViewModel = viewModel(
        factory = TeachersViewModel.Factory(authRepository)
    )
    val reviewsViewModel: ReviewsViewModel = viewModel(
        factory = ReviewsViewModel.Factory(authRepository)
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        bottomBar = {
            NavigationBar(
                containerColor = EmeraldDark,
                contentColor = Color.White
            ) {
                // Tab 0: Home
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Home",
                            tint = if (selectedTab == 0) GoldAccent else Color.White.copy(alpha = 0.7f)
                        )
                    },
                    label = {
                        Text(
                            text = "Home",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 0) GoldAccent else Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = EmeraldPrimary
                    )
                )

                // Tab 1: Courses
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = "Courses",
                            tint = if (selectedTab == 1) GoldAccent else Color.White.copy(alpha = 0.7f)
                        )
                    },
                    label = {
                        Text(
                            text = "Courses",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) GoldAccent else Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = EmeraldPrimary
                    )
                )

                // Tab 2: Library
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = "Library",
                            tint = if (selectedTab == 2) GoldAccent else Color.White.copy(alpha = 0.7f)
                        )
                    },
                    label = {
                        Text(
                            text = "Library",
                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 2) GoldAccent else Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = EmeraldPrimary
                    )
                )

                // Tab 3: Notices & Community
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (notificationsUiState.unreadCount > 0) {
                                    Badge { Text("${notificationsUiState.unreadCount}") }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = "Notices",
                                tint = if (selectedTab == 3) GoldAccent else Color.White.copy(alpha = 0.7f)
                            )
                        }
                    },
                    label = {
                        Text(
                            text = "Notices",
                            fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 3) GoldAccent else Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = EmeraldPrimary
                    )
                )

                // Tab 4: Profile & Account
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = if (selectedTab == 4) GoldAccent else Color.White.copy(alpha = 0.7f)
                        )
                    },
                    label = {
                        Text(
                            text = "Profile",
                            fontWeight = if (selectedTab == 4) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 4) GoldAccent else Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
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
                0 -> HomeScreen(
                    viewModel = homeViewModel,
                    tokenManager = tokenManager,
                    onLogout = onLogout
                )
                1 -> CoursesMainScreen(
                    myCoursesViewModel = myCoursesViewModel,
                    catalogViewModel = coursesCatalogViewModel
                )
                2 -> LibraryMainScreen(
                    libraryViewModel = libraryViewModel,
                    bookstoreViewModel = bookstoreViewModel
                )
                3 -> CommunityMainScreen(
                    announcementsViewModel = announcementsViewModel,
                    blogViewModel = blogViewModel,
                    fatwaViewModel = fatwaViewModel,
                    teachersViewModel = teachersViewModel
                )
                4 -> ProfileMainScreen(
                    profileViewModel = profileViewModel,
                    paymentsViewModel = paymentsViewModel,
                    reviewsViewModel = reviewsViewModel
                )
            }
        }
    }
}
