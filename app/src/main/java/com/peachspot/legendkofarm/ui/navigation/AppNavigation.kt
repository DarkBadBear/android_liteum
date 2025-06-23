package com.peachspot.legendkofarm.ui.navigation


object AppScreenRoutes {
    const val DIARY_SCREEN = "diary_screen" // 경로 이름은 구체적으로 짓는 것이 좋음
    const val PROFILE_SCREEN = "profile_screen"
    const val NOTIFICATION_SCREEN = "notification_screen"
    // ... 필요한 다른 화면 경로들 ...

    // 인자를 받는 경로의 경우
    // const val USER_DETAIL_SCREEN_ROUTE = "user_detail"
    // const val USER_ID_ARG = "userId"
    // fun userDetailScreen(userId: String) = "$USER_DETAIL_SCREEN_ROUTE/$userId"
}
// ... 다른 Composable 화면들 임포트 ...

//@Composable
//fun AppNavigation(homeViewModel: HomeViewModel) {
//    val navController = rememberNavController()
//
//    NavHost(navController = navController, startDestination = AppScreenRoutes.DIARY_SCREEN) { // 사용
//        composable(AppScreenRoutes.DIARY_SCREEN) { // 사용
//            DiaryScreen(viewModel = homeViewModel, navController = navController)
//        }
//        composable(AppScreenRoutes.PROFILE_SCREEN) { // 사용
//            ProfileScreen(viewModel = homeViewModel, navController = navController)
//        }
//
//        // ...
//    }
//}