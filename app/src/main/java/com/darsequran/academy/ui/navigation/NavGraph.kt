package com.darsequran.academy.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.darsequran.academy.data.local.TokenManager
import com.darsequran.academy.data.repository.AuthRepository
import com.darsequran.academy.ui.auth.LoginScreen
import com.darsequran.academy.ui.auth.LoginViewModel
import com.darsequran.academy.ui.auth.RegisterScreen
import com.darsequran.academy.ui.auth.RegisterViewModel
import com.darsequran.academy.ui.auth.SplashScreen
import com.darsequran.academy.ui.home.HomeScreen
import com.darsequran.academy.ui.home.HomeViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController,
    tokenManager: TokenManager,
    authRepository: AuthRepository
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
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
            val viewModel: HomeViewModel = viewModel(
                factory = HomeViewModel.Factory(authRepository)
            )
            HomeScreen(
                viewModel = viewModel,
                tokenManager = tokenManager,
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
