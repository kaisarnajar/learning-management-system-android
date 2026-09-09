package com.darsequran.academy.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.darsequran.academy.ui.blog.BlogViewModel
import com.darsequran.academy.ui.bookstore.BookstoreCartViewModel
import com.darsequran.academy.ui.bookstore.BookstoreViewModel
import com.darsequran.academy.ui.courses.CoursesCatalogViewModel
import com.darsequran.academy.ui.courses.MyCoursesViewModel
import com.darsequran.academy.ui.explore.ExploreMainScreen
import com.darsequran.academy.ui.fatwa.FatwaViewModel
import com.darsequran.academy.ui.home.HomeScreen
import com.darsequran.academy.ui.home.HomeViewModel
import com.darsequran.academy.ui.library.DigitalLibraryViewModel
import com.darsequran.academy.ui.library.LibraryMainScreen
import com.darsequran.academy.ui.payments.FeeWaiverViewModel
import com.darsequran.academy.ui.payments.PaymentsViewModel
import com.darsequran.academy.ui.portal.PortalMainScreen
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
    onNavigateToAbout: () -> Unit,
    onNavigateToContact: () -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(authRepository)
    )

    val myCoursesViewModel: MyCoursesViewModel = viewModel(
        factory = MyCoursesViewModel.Factory(authRepository)
    )
    val coursesCatalogViewModel: CoursesCatalogViewModel = viewModel(
        factory = CoursesCatalogViewModel.Factory(authRepository)
    )
    val teachersViewModel: TeachersViewModel = viewModel(
        factory = TeachersViewModel.Factory(authRepository)
    )

    val libraryViewModel: DigitalLibraryViewModel = viewModel(
        factory = DigitalLibraryViewModel.Factory(authRepository)
    )
    val bookstoreViewModel: BookstoreViewModel = viewModel(
        factory = BookstoreViewModel.Factory(authRepository)
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
    val feeWaiverViewModel: FeeWaiverViewModel = viewModel(
        factory = FeeWaiverViewModel.Factory(authRepository)
    )
    val bookstoreCartViewModel: BookstoreCartViewModel = viewModel(
        factory = BookstoreCartViewModel.Factory(authRepository)
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
                            fontSize = 10.5.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = EmeraldPrimary
                    )
                )

                // Tab 1: Explore (Public Academy Catalog & Community)
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = "Explore",
                            tint = if (selectedTab == 1) GoldAccent else Color.White.copy(alpha = 0.7f)
                        )
                    },
                    label = {
                        Text(
                            text = "Explore",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) GoldAccent else Color.White.copy(alpha = 0.7f),
                            fontSize = 10.5.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = EmeraldPrimary
                    )
                )

                // Tab 2: Library (Digital Library & Bookstore)
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
                            fontSize = 10.5.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = EmeraldPrimary
                    )
                )

                // Tab 3: Dashboard (Student Workspace: My Courses, Payments, Fee Waivers, Cart)
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = "Dashboard",
                            tint = if (selectedTab == 3) GoldAccent else Color.White.copy(alpha = 0.7f)
                        )
                    },
                    label = {
                        Text(
                            text = "Dashboard",
                            fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 3) GoldAccent else Color.White.copy(alpha = 0.7f),
                            fontSize = 10.5.sp,
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
                            fontSize = 10.5.sp,
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
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally(animationSpec = tween(300)) { width -> width } + fadeIn(animationSpec = tween(300)))
                            .togetherWith(slideOutHorizontally(animationSpec = tween(300)) { width -> -width } + fadeOut(animationSpec = tween(300)))
                    } else {
                        (slideInHorizontally(animationSpec = tween(300)) { width -> -width } + fadeIn(animationSpec = tween(300)))
                            .togetherWith(slideOutHorizontally(animationSpec = tween(300)) { width -> width } + fadeOut(animationSpec = tween(300)))
                    }
                },
                label = "StudentPanelTabTransition"
            ) { targetTab ->
                when (targetTab) {
                    0 -> HomeScreen(
                        viewModel = homeViewModel,
                        tokenManager = tokenManager,
                        onNavigateToAbout = onNavigateToAbout,
                        onNavigateToContact = onNavigateToContact,
                        onLogout = onLogout,
                        onNavigateToExplore = { selectedTab = 1 }
                    )
                    1 -> ExploreMainScreen(
                        catalogViewModel = coursesCatalogViewModel,
                        teachersViewModel = teachersViewModel,
                        blogViewModel = blogViewModel,
                        fatwaViewModel = fatwaViewModel,
                        homeViewModel = homeViewModel
                    )
                    2 -> LibraryMainScreen(
                        libraryViewModel = libraryViewModel,
                        bookstoreViewModel = bookstoreViewModel,
                        onNavigateToCart = { selectedTab = 3 }
                    )
                    3 -> PortalMainScreen(
                        myCoursesViewModel = myCoursesViewModel,
                        paymentsViewModel = paymentsViewModel,
                        feeWaiverViewModel = feeWaiverViewModel,
                        bookstoreCartViewModel = bookstoreCartViewModel,
                        onNavigateToExplore = { selectedTab = 1 }
                    )
                    4 -> ProfileMainScreen(
                        profileViewModel = profileViewModel,
                        paymentsViewModel = paymentsViewModel,
                        reviewsViewModel = reviewsViewModel,
                        tokenManager = tokenManager,
                        onLogout = onLogout
                    )
                }
            }
        }
    }
}
