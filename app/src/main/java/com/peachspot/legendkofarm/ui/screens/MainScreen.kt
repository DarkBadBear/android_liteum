// MainScreen.kt
package com.peachspot.legendkofarm.ui.screens

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.webkit.ValueCallback
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.firebase.auth.FirebaseAuth
import com.peachspot.legendkofarm.R
import com.peachspot.legendkofarm.data.db.AppDatabase
import com.peachspot.legendkofarm.data.remote.client.NetworkClient.myApiService
import com.peachspot.legendkofarm.data.repositiory.HomeRepositoryImpl
import com.peachspot.legendkofarm.data.repositiory.UserPreferencesRepository
import com.peachspot.legendkofarm.viewmodel.HomeViewModel
import com.peachspot.legendkofarm.ui.navigation.AppScreenRoutes


data class BottomNavigationItem(
    val labelResId: Int, val iconResId: Int, val screenRoute: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavHostController,
    homeViewModel: HomeViewModel,
    onFileChooserRequest: (ValueCallback<Array<Uri>>, Intent) -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"

    val navItems = listOf(
        BottomNavigationItem(R.string.tab_label_building, R.drawable.ic_house, "home"),
        BottomNavigationItem(R.string.tab_diary, R.drawable.ic_diary, "diary"),
        BottomNavigationItem(R.string.tab_exchange, R.drawable.ic_exchange, "exchange"),
        BottomNavigationItem(R.string.tab_news, R.drawable.ic_news, "news"),
        BottomNavigationItem(R.string.tab_profile, R.drawable.ic_profile, "profile")
    )

    LaunchedEffect(currentRoute) {
        selectedTab = when (currentRoute) {
            "home" -> 0
            "diary" -> 1
            "exchange" -> 2
            "news" -> 3
            "profile", AppScreenRoutes.PROFILE_SCREEN -> 4
            else -> selectedTab
        }
    }


    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF535353)) {
                navItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                painter = painterResource(id = item.iconResId),
                                contentDescription = stringResource(id = item.labelResId),
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { Text(stringResource(id = item.labelResId)) },
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            navController.navigate(item.screenRoute) {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            unselectedIconColor = Color.Gray,
                            selectedTextColor = Color.White,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable("home") {
                HomeScreen(
                    navController = navController,
                    viewModel = homeViewModel,
                    onFileChooserRequest = onFileChooserRequest
                )
            }
            composable("diary") {
                DiaryScreen(
                    navController = navController,
                    viewModel = homeViewModel,
                    onFileChooserRequest = onFileChooserRequest
                )
            }
            composable("exchange") {
                ExchangeScreen(
                    navController = navController,
                    viewModel = homeViewModel,
                    onFileChooserRequest = onFileChooserRequest
                )
            }
            composable("news") {
                NewsScreen(
                    navController = navController,
                    viewModel = homeViewModel,
                    onFileChooserRequest = onFileChooserRequest
                )
            }
            composable("profile") {
                ProfileScreen(
                    navController = navController,
                    viewModel = homeViewModel
                )
            }
            composable(AppScreenRoutes.NOTIFICATION_SCREEN) {
                NotificationScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

        }
    }
}


/*
//0c275a
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onFileChooserRequest: (ValueCallback<Array<Uri>>, Intent) -> Unit
) {
    val navController = rememberNavController()
    var selectedTab by rememberSaveable { mutableStateOf(0) }


    val navItems = listOf(
        BottomNavigationItem(R.string.tab_label_building, R.drawable.ic_house, "홈"),
        BottomNavigationItem(R.string.tab_diary, R.drawable.ic_diary, "농업일지"),
        BottomNavigationItem(R.string.tab_exchange, R.drawable.ic_exchange, "거래시세"),
        BottomNavigationItem(R.string.tab_news, R.drawable.ic_news, "소식"),
        BottomNavigationItem(R.string.tab_profile, R.drawable.ic_profile, "프로필")
    )

    val application = LocalContext.current.applicationContext as Application
    val database = remember { AppDatabase.getInstance(application) } // remember for stability
    val userPreferencesRepository = remember { UserPreferencesRepository(application) } // remember
    val firebaseAuthInstance = remember { FirebaseAuth.getInstance() } // remember

    // ViewModel for HomeScreen
    val homeRepository: HomeRepository =
        remember { HomeRepositoryImpl(database.farmLogDao()) }
    val homeViewModelFactory = remember {
        HomeViewModelFactory(
            application,
            userPreferencesRepository,
            firebaseAuthInstance,
            myApiService,
            homeRepository
        )
    }
    val homeViewModel: HomeViewModel = viewModel(factory = homeViewModelFactory)

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF535353) ,

            ) {
                navItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                painter = painterResource(id = item.iconResId),
                                contentDescription = stringResource(id = item.labelResId),
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { Text(stringResource(id = item.labelResId)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            unselectedIconColor = Color.Gray,
                            selectedTextColor = Color.White,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color.Transparent // 선택시 배경 강조 없애려면
                        ),
                        selected = selectedTab == index,
                        onClick = { selectedTab = index })
                }
            }
        }) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = "home_tab_host", // 기본적으로 탭 콘텐츠를 보여주는 라우트
            modifier = Modifier.padding(innerPadding),

        ) {
            composable("home_tab_host") {
                val screenModifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)

                when (selectedTab) {
                    0 -> HomeScreen(
                        viewModel = homeViewModel,
                        navController = navController,
                        onFileChooserRequest = onFileChooserRequest
                    )

                    1 -> DiaryScreen(
                        viewModel = homeViewModel,
                        navController = navController,
                        onFileChooserRequest = onFileChooserRequest
                    )

                    2 -> ExchangeScreen(
                        viewModel = homeViewModel,
                        navController = navController,
                        onFileChooserRequest = onFileChooserRequest
                    )

                    3 -> NewsScreen(
                        viewModel = homeViewModel,
                        navController = navController,
                        onFileChooserRequest = onFileChooserRequest
                    )

                    4 -> ProfileScreen(
                        viewModel = homeViewModel,
                        navController = navController
                            //onFileChooserRequest = onFileChooserRequest

                    )

                }
            }

            composable(AppScreenRoutes.PROFILE_SCREEN) { // 사용
                ProfileScreen(viewModel = homeViewModel,navController = navController)
            }

            composable("notification_screen_route") {
                NotificationScreen(onNavigateBack = { navController.popBackStack() })
            }

        }
    }
}


@Composable
fun AppNavHost(
    navController: NavHostController,
    homeViewModel: HomeViewModel, // 예시로 HomeScreen에 필요
    onFileChooserRequest: (ValueCallback<Array<Uri>>, Intent) -> Unit
    // ...
) {
    NavHost(navController = navController, startDestination = "main_screen_route") { // 시작 화면 라우트
        composable("main_screen_route") {
            HomeScreen(viewModel = homeViewModel, navController = navController,onFileChooserRequest = onFileChooserRequest)
        }
        composable("notification_screen_route") {
            NotificationScreen(onNavigateBack = { navController.popBackStack() })
        }
        // ... (다른 화면 라우트들)
    }
}

*/