package com.peachspot.smartkofarm.ui.screens

import com.peachspot.smartkofarm.R
import android.app.Application
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import com.peachspot.smartkofarm.data.db.AppDatabase
import com.peachspot.smartkofarm.data.remote.client.NetworkClient.myApiService
import com.peachspot.smartkofarm.data.repositiory.HomeRepository
import com.peachspot.smartkofarm.data.repositiory.HomeRepositoryImpl
import com.peachspot.smartkofarm.data.repositiory.UserPreferencesRepository
import com.peachspot.smartkofarm.viewmodel.HomeViewModel
import com.peachspot.smartkofarm.viewmodel.HomeViewModelFactory


data class BottomNavigationItem(
    val labelResId: Int, val iconResId: Int, val screenRoute: String
)

//0c275a
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    var selectedTab by rememberSaveable { mutableStateOf(0) }


    val navItems = listOf(
        BottomNavigationItem(R.string.tab_label_building,  R.drawable.ic_house, "home"),
 //        BottomNavigationItem(R.string.tab_label_mountain, R.drawable.ic_mountain, "mountain"),
//        BottomNavigationItem(R.string.tab_label_stairway, R.drawable.ic_stairway, "stairway"),
//        BottomNavigationItem(R.string.tab_label_profile, R.drawable.ic_profile, "profile")
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
            homeRepository, userPreferencesRepository, myApiService
        )
    }
    val homeViewModel: HomeViewModel = viewModel(factory = homeViewModelFactory)

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFFe1e1e1)   ///MaterialTheme.colorScheme.surface
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
                        selected = selectedTab == index,
                        onClick = { selectedTab = index })
                }
            }
        }) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = "home_tab_host", // 기본적으로 탭 콘텐츠를 보여주는 라우트
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home_tab_host") {
                val screenModifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)

                when (selectedTab) {
                    0 -> HomeScreen(
                        viewModel = homeViewModel,
                        navController = navController
                    ) // HomeScreen에 NavController 전달

                }
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
    // ... (필요한 ViewModel 인스턴스들)
    homeViewModel: HomeViewModel, // 예시로 HomeScreen에 필요
    // ...
) {
    NavHost(navController = navController, startDestination = "main_screen_route") { // 시작 화면 라우트
        composable("main_screen_route") {
            HomeScreen(viewModel = homeViewModel, navController = navController)
        }
        composable("notification_screen_route") {
          //  NotificationScreen(onNavigateBack = { navController.popBackStack() })
        }
        // ... (다른 화면 라우트들)
    }
}

