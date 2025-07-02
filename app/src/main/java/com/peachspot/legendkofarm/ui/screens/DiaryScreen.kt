package com.peachspot.legendkofarm.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.*
import android.widget.ImageView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.peachspot.legendkofarm.R
import com.peachspot.legendkofarm.ui.components.MyAppTopBar
import com.peachspot.legendkofarm.viewmodel.HomeViewModel
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat.startActivityForResult
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted

import com.peachspot.legendkofarm.MainActivity
import com.peachspot.legendkofarm.ui.components.CommonWebView

import com.peachspot.legendkofarm.ui.navigation.AppScreenRoutes
import com.peachspot.legendkofarm.util.Logger
import com.peachspot.legendkofarm.viewmodel.HomeUiState


@OptIn(ExperimentalPermissionsApi::class,ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DiaryScreen(
    viewModel: HomeViewModel,
    navController: NavController,
    modifier: Modifier = Modifier,
    onFileChooserRequest: (ValueCallback<Array<Uri>>, Intent) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val activity = context as? Activity

    var showUrlPopup by remember { mutableStateOf(false) }
    var popupUrl by remember { mutableStateOf<String?>(null) }
    var webViewForPopup by remember { mutableStateOf<WebView?>(null) }

    var showAlert by remember { mutableStateOf(false) }
    var alertMessage by remember { mutableStateOf<String?>(null) }
    var jsAlertResult by remember { mutableStateOf<JsResult?>(null) }

    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    LaunchedEffect(cameraPermissionState.status) {
        if (!cameraPermissionState.status.isGranted) {
            Log.d("DiaryScreen", "Requesting camera permission")
            cameraPermissionState.launchPermissionRequest()
        }
    }

    if (cameraPermissionState.status.isGranted) {
        Text("카메라 권한 허용됨")
    } else {
        Text("카메라 권한 필요")
        Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
            Text("권한 요청")
        }
    }


    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearUserMessage()
        }
    }


    Scaffold(
        contentWindowInsets = WindowInsets(0), // ← 상하 모두 insets 제거
        containerColor = Color.White, // Scaffold 배경
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { snackbarData ->
                val containerColor = when (uiState.userMessageType) {
                    HomeScreenContentTypes.INFO, HomeScreenContentTypes.SUCCESS -> Color(0xFF4CAF50)
                    HomeScreenContentTypes.ERROR -> Color(0xFFFF0000)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                val contentColor = when (uiState.userMessageType) {
                    HomeScreenContentTypes.ERROR -> MaterialTheme.colorScheme.onError
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Snackbar(
                    snackbarData = snackbarData,
                    containerColor = containerColor,
                    contentColor = contentColor
                )
            }
        },
        topBar = {
            MyAppTopBar(
                title = stringResource(R.string.screen_title_building),
                onNotificationClick = {
                    navController.navigate(AppScreenRoutes.NOTIFICATION_SCREEN)

                },
                onRefreshClicked ={
                    viewModel.refreshWebView("diary")
                },
                onTitleClick = {

//                    navController.navigate("home_tab_host") {
//                        //popUpTo("home") { inclusive = true }
//                    }
                }
            )
        }

    ) { innerPadding ->
        Box(
            modifier = modifier
                //.padding(innerPadding)
                .fillMaxSize()
        ) {


            if (!uiState.isUserLoggedIn) {
                // 로그인 화면
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {


                    Spacer(Modifier.height(24.dp)) // 상단 여백

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp), // 아이콘 간 간격
                        verticalAlignment = Alignment.CenterVertically,     // 수직 중앙 정렬
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.notebook_pen),
                            contentDescription = "Icon 1",
                            modifier = Modifier.size(48.dp)
                        )

                        Image(
                            painter = painterResource(id = R.drawable.ellipsis),
                            contentDescription = "Icon 2",
                            modifier = Modifier.size(48.dp)
                        )

                        Image(
                            painter = painterResource(id = R.drawable.database),
                            contentDescription = "Icon 3",
                            modifier = Modifier.size(48.dp)
                        )
                    }



                    Spacer(Modifier.height(24.dp)) // 상단 여백

                    Text(
                        text = "구글 연동 이후 사용가능 합니다.",
                        color = Color.Black
                    )


                    Button(
                        onClick = {
                            navController.navigate("profile") {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo("home") { saveState = true }
                            }
                        }
                    ) {
                        Text("이동")
                    }



                }
            } else {

                /*새로고침 방지 모델
                val webView = remember {
                    viewModel.getOrCreateWebView(
                        context = context,
                        tag = "diary", // 고유 키 (탭별로 다르게 설정하세요)
                        url = "https://urdesk.co.kr/smartkofarmdiary?uid={${uiState.firebaseUid}",
                    )
                } */

                val webView = viewModel.getOrCreateWebView(
                        context = context,
                        tag = "diary", // 고유 키 (탭별로 다르게 설정하세요)
                        url = "https://urdesk.co.kr/smartkofarmdiary?uid={${uiState.firebaseUid}",
                    )

                CommonWebView(
                    webView = webView,
                    modifier = modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    onShowPopup = { url, webView ->
                        popupUrl = url
                        webViewForPopup = webView
                        showUrlPopup = true
                    },
                    onJsAlert = { message, result ->
                        alertMessage = message
                        jsAlertResult = result
                        showAlert = true
                    },
                    onFileChooserRequest = onFileChooserRequest
                )
            }


        }
    }
}


@Composable
fun LoggedInUserProfile(uiState: HomeUiState, onSignOutClicked: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Logger.d("NICAP", uiState.toString());
        /*uiState.userPhotoUrl?.let {
            AsyncImage(
                model = it,
                contentDescription = "User Profile Photo",
                modifier = Modifier.size(100.dp)
            )
            Spacer(Modifier.height(16.dp))
        }
        uiState.userName?.let { Text("이름: $it", style = MaterialTheme.typography.headlineSmall) }
        uiState.userEmail?.let { Text("이메일: $it", style = MaterialTheme.typography.bodyLarge) }
        Spacer(Modifier.height(24.dp))*/

        Button(onClick = onSignOutClicked) { Text("Google 로그아웃") }
    }
}


@Composable
fun LoginPrompt(onSignInClicked: () -> Unit, enabled: Boolean = true) { // enabled 파라미터 추가 및 기본값 설정
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onSignInClicked,
            enabled = enabled // 버튼 활성화 상태를 파라미터로 제어
        ) {
            Text("Google 연동")
        }
    }
}
