package com.peachspot.liteum.ui.screens

import android.content.Intent
import android.net.Uri
import android.util.Log // Log import 추가
import android.webkit.ValueCallback
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext // LocalContext import 추가
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.peachspot.liteum.data.db.AppDatabase // 가정: DB 접근 클래스
import com.peachspot.liteum.data.remote.api.MyApiService
import com.peachspot.liteum.data.repositiory.BookRepository // Repository 인터페이스
import com.peachspot.liteum.data.repositiory.BookRepositoryImpl // Repository 구현체
import com.peachspot.liteum.viewmodel.FeedViewModel
import com.peachspot.liteum.viewmodel.HomeViewModel
import kotlinx.coroutines.delay

@Composable
fun MainScreen(
    navController: NavHostController,
    homeViewModel: HomeViewModel,
    feedViewModel: FeedViewModel,
    myApiService: MyApiService,
    onFileChooserRequest: (ValueCallback<Array<Uri>>, Intent) -> Unit,
) {
    val authUiState by homeViewModel.uiState.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "loading"

    val context = LocalContext.current

    // BookRepository 인스턴스 생성 (DI 프레임워크 미사용 시 예시)
    // 실제 앱에서는 Hilt나 Koin 같은 DI 프레임워크를 사용하거나,
    // Application 클래스에서 싱글톤으로 관리하는 것이 더 좋습니다.
    val bookRepository: BookRepository = remember {
        Log.d("MainScreen", "Creating BookRepository instance") // 인스턴스 생성 로깅
        val appDatabase = AppDatabase.getInstance(context.applicationContext)
        BookRepositoryImpl(
            bookLogsDao = appDatabase.bookLogsDao(),
            reviewLogsDao = appDatabase.reviewLogsDao(),
            myApiService = myApiService
            // BookRepositoryImpl의 생성자에 필요한 다른 DAO가 있다면 추가해야 합니다.
        )
    }

    NavHost(
        navController = navController,
        startDestination = "loading",
        modifier = Modifier.fillMaxSize()
    ) {
        composable("loading") {
            LoadingScreen()
            LaunchedEffect(authUiState.isLoading, authUiState.isUserLoggedIn) { // isUserLoggedIn도 키로 추가하여 로그인 상태 변경 시 재실행
                if (!authUiState.isLoading) {
                    // delay(200L) // 로그인 상태 확인 후 지연은 불필요할 수 있음
                    val destination = if (authUiState.isUserLoggedIn) "home" else "login"
                    navController.navigate(destination) {
                        popUpTo("loading") { inclusive = true }
                        launchSingleTop = true // 중복 화면 방지
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
                    popUpTo(navController.graph.startDestinationId) { inclusive = true } // 그래프의 시작점까지 pop
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

        composable("review") { // 이 화면은 '리뷰 목록' 또는 '리뷰 작성'일 수 있습니다.
            // 만약 '리뷰 작성'이고 특정 책 ID가 필요 없다면 그대로 유지.
            // 특정 책에 대한 리뷰 작성이라면 bookId 파라미터가 필요할 수 있습니다.
            ReviewScreen( // 이 화면이 구체적으로 어떤 역할을 하는지 확인 필요
                navController = navController,
                viewModel = homeViewModel,
                bookRepository = bookRepository // 생성된 BookRepository 인스턴스 전달
            )
        }

        composable("byebye") {
            ByeScreen()
        }

        composable(
            route = "review_edit/{bookLogId}", // reviewId -> bookLogId로 변경하여 의미 명확화
            arguments = listOf(navArgument("bookLogId") { // reviewId -> bookLogId
                type = NavType.LongType
                // defaultValue = 0L // 필수 인자라면 defaultValue를 제거하거나, 유효하지 않은 값으로 처리
            })
        ) { backStackEntry ->
            // NavArgument에서 defaultValue를 사용하지 않으면 null을 반환할 수 있으므로 안전하게 처리
            val bookLogIdArg = backStackEntry.arguments?.getLong("bookLogId")

            if (bookLogIdArg != null && bookLogIdArg > 0L) { // 유효한 ID인지 확인
                ReviewEditScreen(
                    navController = navController,
                    viewModel = homeViewModel,
                    bookLogId = bookLogIdArg, // 전달된 bookLogId 사용
                    bookRepository = bookRepository // 생성된 BookRepository 인스턴스 전달
                )
            } else {
                // 유효하지 않은 ID 또는 ID가 없는 경우 처리
                Log.e("MainScreen NavGraph", "Invalid or missing bookLogId for review_edit route. Popping back.")
                // 사용자에게 알림을 주거나, 이전 화면으로 안전하게 이동시키는 것이 좋습니다.
                LaunchedEffect(Unit) { // Composable 내부에서 navigate/popBackStack 호출 시 LaunchedEffect 사용 권장
                    navController.popBackStack()
                }
                // 또는 에러 화면으로 이동할 수도 있습니다.
                // Text("잘못된 접근입니다. 책 ID가 필요합니다.")
            }
        }
    }
}

