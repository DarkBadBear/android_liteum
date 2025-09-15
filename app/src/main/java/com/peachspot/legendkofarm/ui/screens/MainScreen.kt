package com.peachspot.legendkofarm.ui.screens

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.webkit.ValueCallback
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.peachspot.legendkofarm.viewmodel.HomeViewModel
import com.peachspot.legendkofarm.ui.screens.LoadingScreen
import com.peachspot.legendkofarm.ui.screens.LoginScreen
import kotlinx.coroutines.delay

@Composable
fun MainScreen(
    navController: NavHostController,
    homeViewModel: HomeViewModel,
    onFileChooserRequest: (ValueCallback<Array<Uri>>, Intent) -> Unit
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
                     //       launchSingleTop = true
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
                onFileChooserRequest = onFileChooserRequest
            )

            // 홈 화면에서 로그아웃 시 즉시 로그인 화면으로 이동
            /*LaunchedEffect(authUiState.isUserLoggedIn) {
                if (!authUiState.isUserLoggedIn) {
                    navController.navigate("login") {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }*/

        }

        composable("byebye") {
            ByeScreen()
        }

    }
}
