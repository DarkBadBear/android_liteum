package com.peachspot.legendkofarm.ui.screens

import com.peachspot.legendkofarm.R
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.webkit.ValueCallback
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.peachspot.legendkofarm.data.db.AppDatabase
import com.peachspot.legendkofarm.data.remote.client.NetworkClient.myApiService
import com.peachspot.legendkofarm.data.repositiory.HomeRepository
import com.peachspot.legendkofarm.data.repositiory.HomeRepositoryImpl
import com.peachspot.legendkofarm.data.repositiory.UserPreferencesRepository
import com.peachspot.legendkofarm.viewmodel.HomeViewModel
import com.peachspot.legendkofarm.viewmodel.HomeViewModelFactory

import com.peachspot.legendkofarm.ui.navigation.AppScreenRoutes

data class BottomNavigationItem(
    val labelResId: Int, val iconResId: Int, val screenRoute: String
)

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

