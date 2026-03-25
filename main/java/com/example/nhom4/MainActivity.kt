package com.example.nhom4

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.nhom4.ui.*
import com.example.nhom4.ui.theme.Nhom4Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Nhom4Theme {
                ShoppingApp()
            }
        }
    }
}

@Composable
fun ShoppingApp() {
    val navController = rememberNavController()
    val viewModel: ShoppingViewModel = viewModel()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onProductClick = { productId ->
                    navController.navigate("product_detail/$productId")
                },
                onCartClick = {
                    if (isLoggedIn) {
                        navController.navigate("checkout")
                    } else {
                        navController.navigate("login")
                    }
                },
                onLogout = { viewModel.logout() }
            )
        }

        composable("login") {
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            "product_detail/{productId}",
            arguments = listOf(navArgument("productId") { type = NavType.IntType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getInt("productId") ?: return@composable
            ProductDetailScreen(
                productId = productId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onLoginRequired = {
                    navController.navigate("login")
                }
            )
        }

        composable("checkout") {
            CheckoutScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onPaid = {
                    navController.navigate("order_summary") {
                        popUpTo("home") { inclusive = false }
                    }
                }
            )
        }

        composable("order_summary") {
            OrderSummaryScreen(
                onContinue = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }
    }
}
