package com.darsequran.academy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.darsequran.academy.data.local.TokenManager
import com.darsequran.academy.data.remote.RetrofitClient
import com.darsequran.academy.data.repository.AuthRepository
import com.darsequran.academy.ui.navigation.AppNavGraph
import com.darsequran.academy.ui.theme.DarseQuranAcademyTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Ensure white/light status bar icons on dark emerald header
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false

        val tokenManager = TokenManager(applicationContext)
        val authApi = RetrofitClient.getAuthApi(tokenManager)
        val authRepository = AuthRepository(authApi, tokenManager)

        setContent {
            DarseQuranAcademyTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    AppNavGraph(
                        navController = navController,
                        tokenManager = tokenManager,
                        authRepository = authRepository
                    )
                }
            }
        }
    }
}
