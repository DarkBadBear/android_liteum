package com.peachspot.liteum.ui.screens

import android.content.Intent
import android.net.Uri
import android.webkit.ValueCallback
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.peachspot.liteum.viewmodel.FeedViewModel

import com.peachspot.liteum.viewmodel.HomeViewModel
import kotlinx.coroutines.delay

@Composable
fun MainScreen(
    navController: NavHostController,
    homeViewModel: HomeViewModel,
    feedViewModel: FeedViewModel,
    onFileChooserRequest: (ValueCallback<Array<Uri>>, Intent) -> Unit,

) {
    val authUiState by homeViewModel.uiState.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "loading"

    NavHost(
        navController = navController,
        startDestination = "loading",
        modifier = Modifier.fillMaxSize()
    ) {
        composable("loading") {
            LoadingScreen()
            // 로그인 상태 감시 및 라우팅
            LaunchedEffect(authUiState.isLoading) {

                if (!authUiState.isLoading) {
                    delay(200L)
                    if (authUiState.isUserLoggedIn) {

                        navController.navigate("home") {0
                            popUpTo("loading") { inclusive = true }
                        }

                    } else {
                        navController.navigate("login") {
                            popUpTo("loading") { inclusive = true }
//                   launchSingleTop = true
                        }
                    }
                }

            }
        }

        composable("login") {
            LoginScreen(
                navController = navController,
                viewModel = homeViewModel
            ) {
                navController.navigate("home") {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }

        composable("home") {
            HomeScreen(
                navController = navController,
                viewModel = homeViewModel,
                feedViewModel =  feedViewModel,
            )
        }
        composable("review") {
            ReviewScreen(
                navController = navController,
                viewModel = homeViewModel,
            )
        }

        composable("byebye") {
            ByeScreen()
        }
        // NavGraphBuilder 확장 함수 내 또는 NavHost 내
        // Navigation 설정 예시
        composable(
            route = "review_edit/{reviewId}",
            arguments = listOf(navArgument("reviewId") {
                type = NavType.LongType
                defaultValue = 0L
            })
        ) { backStackEntry ->
            val reviewId = backStackEntry.arguments?.getLong("reviewId") ?: 0L
            ReviewEditScreen(
                navController = navController,
                viewModel = homeViewModel,
                reviewId = reviewId
            )
        }



    }
}
