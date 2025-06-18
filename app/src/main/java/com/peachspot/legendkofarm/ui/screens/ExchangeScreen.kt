package com.peachspot.legendkofarm.ui.screens

import android.annotation.SuppressLint
import android.webkit.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.peachspot.legendkofarm.ui.components.MyAppTopBar
import com.peachspot.legendkofarm.viewmodel.HomeViewModel
import com.peachspot.legendkofarm.ui.components.CommonWebView



@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ExchangeScreen(
    viewModel: HomeViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var showUrlPopup by remember { mutableStateOf(false) }
    var popupUrl by remember { mutableStateOf<String?>(null) }
    var webViewForPopup by remember { mutableStateOf<WebView?>(null) }

    var showAlert by remember { mutableStateOf(false) }
    var alertMessage by remember { mutableStateOf<String?>(null) }
    var jsAlertResult by remember { mutableStateOf<JsResult?>(null) }
//
//    val coroutineScope = rememberCoroutineScope()
//    var isRefreshing by remember { mutableStateOf(false) }
//
//    val webView = remember {
//        WebView(context).apply {
//            settings.javaScriptEnabled = true
//            settings.domStorageEnabled = true
//            settings.javaScriptCanOpenWindowsAutomatically = true
//            settings.setSupportMultipleWindows(true)
//            settings.loadWithOverviewMode = true
//            settings.useWideViewPort = true
//            loadUrl("https://urdesk.co.kr/smartkofarm/diary")
//        }
//    }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearUserMessage()
        }
    }

//
//
//    if (showAlert) {
//        AlertDialog(
//            onDismissRequest = {
//                jsAlertResult?.cancel()
//                showAlert = false
//            },
//            title = { Text("알림") },
//            text = { Text(alertMessage ?: "") },
//            confirmButton = {
//                TextButton(onClick = {
//                    jsAlertResult?.confirm()
//                    showAlert = false
//                }) {
//                    Text("확인")
//                }
//            }
//        )
//    }

    Scaffold(
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
                onTitleClick = {
                    // 👉 홈 화면으로 이동
                    navController.navigate("home_tab_host") {
                       // popUpTo("home") { inclusive = true }
                    }
                }
            )
        }

    ) { innerPadding ->
        Box(modifier = modifier.padding(innerPadding).fillMaxSize()) {
            CommonWebView(
                url = "https://urdesk.co.kr/smartkofarmexchange",
                modifier = modifier
                    //.padding(innerPadding)
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
                }
            )
        }
    }
}
