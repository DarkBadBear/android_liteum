package com.peachspot.liteum.ui.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.ValueCallback
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
// import androidx.compose.ui.platform.LocalContext // Context가 직접 필요 없다면 제거 가능
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
// import com.peachspot.liteum.data.db.AppDatabase // MainScreen에서 직접 사용 안 함
// import com.peachspot.liteum.data.remote.api.MyApiService // MainScreen에서 직접 사용 안 한다면 제거
import com.peachspot.liteum.data.repositiory.BookRepository // Repository 인터페이스는 타입으로 필요
// import com.peachspot.liteum.data.repositiory.BookRepositoryImpl // MainScreen에서 직접 생성 안 함
import com.peachspot.liteum.viewmodel.BookSearchViewModel // BookSearchViewModel 추가 (필요시)
import com.peachspot.liteum.viewmodel.FeedViewModel
import com.peachspot.liteum.viewmodel.HomeViewModel
// import kotlinx.coroutines.delay // 현재 사용되지 않음

@Composable
fun MainScreen(
    navController: NavHostController,
    homeViewModel: HomeViewModel,
    feedViewModel: FeedViewModel,
    bookSearchViewModel: BookSearchViewModel, // BookSearchViewModel 파라미터 추가
    bookRepository: BookRepository, // MainActivity로부터 BookRepository 인스턴스를 전달받음
    // myApiService: MyApiService, // MyApiService를 직접 사용하지 않는다면 제거
    onFileChooserRequest: (ValueCallback<Array<Uri>>, Intent) -> Unit,
) {
    val authUiState by homeViewModel.uiState.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "loading"

    // LocalContext가 MainScreen 레벨에서 특별히 필요 없다면 이 변수는 제거해도 됩니다.
    // 하위 Composable에서 필요시 LocalContext.current를 직접 호출 가능합니다.
    // val context = LocalContext.current

    // BookRepository 인스턴스를 MainScreen 내부에서 직접 생성하는 코드 삭제
    // val bookRepository: BookRepository = remember { ... } // 이 블록 전체 삭제

    NavHost(
        navController = navController,
        startDestination = "loading",
        modifier = Modifier.fillMaxSize()
    ) {
        composable("loading") {
            LoadingScreen()
            LaunchedEffect(authUiState.isLoading, authUiState.isUserLoggedIn) {
                if (!authUiState.isLoading) {
                    val destination = if (authUiState.isUserLoggedIn) "home" else "login"
                    navController.navigate(destination) {
                        popUpTo("loading") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }

        composable("login") {
            LoginScreen(
                navController = navController,
                viewModel = homeViewModel
            ) { // onLoginSuccess 람다
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
                feedViewModel = feedViewModel,
            )
        }

        composable("review") {
            // ReviewScreen이 BookRepository를 필요로 한다면 전달
            ReviewScreen(
                navController = navController,
                viewModel = homeViewModel, // 또는 ReviewViewModel 사용
                bookRepository = bookRepository // 전달받은 bookRepository 사용
            )
        }

        composable("byebye") {
            ByeScreen()
        }

        composable(
            route = "review_edit/{bookLogId}",
            arguments = listOf(navArgument("bookLogId") {
                type = NavType.LongType
            })
        ) { backStackEntry ->
            val bookLogIdArg = backStackEntry.arguments?.getLong("bookLogId")

            if (bookLogIdArg != null && bookLogIdArg > 0L) {
                // ReviewEditScreen이 BookRepository를 필요로 한다면 전달
                ReviewEditScreen(
                    navController = navController,
                    viewModel = homeViewModel, // 또는 ReviewEditViewModel 사용
                    bookLogId = bookLogIdArg,
                    bookRepository = bookRepository // 전달받은 bookRepository 사용
                )
            } else {
                Log.e("MainScreen NavGraph", "Invalid or missing bookLogId for review_edit route. Popping back.")
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            }
        }

    }
}
