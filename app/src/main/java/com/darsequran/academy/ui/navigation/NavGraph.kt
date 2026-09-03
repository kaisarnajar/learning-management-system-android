package com.darsequran.academy.ui.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.darsequran.academy.data.local.TokenManager
import com.darsequran.academy.data.repository.AuthRepository
import com.darsequran.academy.ui.about.AboutScreen
import com.darsequran.academy.ui.auth.LoginScreen
import com.darsequran.academy.ui.auth.LoginViewModel
import com.darsequran.academy.ui.auth.RegisterScreen
import com.darsequran.academy.ui.auth.RegisterViewModel
import com.darsequran.academy.ui.auth.SplashScreen
import com.darsequran.academy.ui.contact.ContactScreen
import com.darsequran.academy.ui.contact.ContactViewModel
import com.darsequran.academy.ui.main.StudentPanelScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    tokenManager: TokenManager,
    authRepository: AuthRepository
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(durationMillis = 340))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth / 4 },
                animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(durationMillis = 340))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -fullWidth / 4 },
                animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(durationMillis = 340))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(durationMillis = 340))
        }
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                tokenManager = tokenManager,
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            val viewModel: LoginViewModel = viewModel(
                factory = LoginViewModel.Factory(authRepository)
            )
            LoginScreen(
                viewModel = viewModel,
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            val viewModel: RegisterViewModel = viewModel(
                factory = RegisterViewModel.Factory(authRepository)
            )
            RegisterScreen(
                viewModel = viewModel,
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            StudentPanelScreen(
                tokenManager = tokenManager,
                authRepository = authRepository,
                onNavigateToAbout = {
                    navController.navigate(Screen.About.route)
                },
                onNavigateToContact = {
                    navController.navigate(Screen.Contact.route)
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.About.route) {
            AboutScreen(
                onBackPress = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Contact.route) {
            val contactViewModel: ContactViewModel = viewModel(
                factory = ContactViewModel.Factory(authRepository)
            )
            ContactScreen(
                viewModel = contactViewModel,
                tokenManager = tokenManager,
                onBackPress = {
                    navController.popBackStack()
                }
            )
        }
    }
}
