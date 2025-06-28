package com.peachspot.legendkofarm.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.webkit.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.peachspot.legendkofarm.MainActivity
import com.peachspot.legendkofarm.ui.components.MyAppTopBar
import com.peachspot.legendkofarm.viewmodel.HomeViewModel
import com.peachspot.legendkofarm.ui.components.CommonWebView
import com.peachspot.legendkofarm.R
import com.peachspot.legendkofarm.ui.navigation.AppScreenRoutes

object HomeScreenContentTypes {
    const val INFO = "info"
    const val SUCCESS = "success"
    const val ERROR = "error"
}


@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HomeScreen(
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
            SnackbarHost(hostState = snackbarHostState) {
                snackbarData ->
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
                    viewModel.refreshWebView("home")
                },
                onTitleClick = {

                }
            )
        }

    ) { innerPadding ->
        Box(modifier = modifier.padding(innerPadding).fillMaxSize()) {
            val webView = remember {
                viewModel.getOrCreateWebView(
                    context = context,
                    tag = "home", // 고유 키 (탭별로 다르게 설정하세요)
                    url = "https://urdesk.co.kr/smartkofarm?uid={${uiState.firebaseUid}",
                )
            }

            CommonWebView(
                webView = webView,
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
                },
                onFileChooserRequest = onFileChooserRequest
            )
        }
    }
}
