package com.peachspot.smartkofarm.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Message
import android.util.Log
import android.webkit.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.peachspot.smartkofarm.R
import com.peachspot.smartkofarm.ui.components.MyAppTopBar
import com.peachspot.smartkofarm.viewmodel.HomeViewModel
import kotlinx.coroutines.launch
import java.net.URI
import android.widget.LinearLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout


object HomeScreenContentTypes {
    const val INFO = "info"
    const val SUCCESS = "success"
    const val ERROR = "error"
}

object AppScreenRoutes {
    const val NOTIFICATION_SCREEN = "notification_screen_route"
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HomeScreen(
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

    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.setSupportMultipleWindows(true)
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            loadUrl("https://urdesk.co.kr/smartkofarm")
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

    if (showUrlPopup && popupUrl != null) {
        AlertDialog(
            onDismissRequest = { showUrlPopup = false },
            title = { Text("새 창 열기") },
            text = { Text("'$popupUrl' 주소의 새 창을 여시겠습니까?") },
            confirmButton = {
                TextButton(onClick = {
                    webViewForPopup?.loadUrl(popupUrl!!)
                    showUrlPopup = false
                }) {
                    Text("열기")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUrlPopup = false }) {
                    Text("취소")
                }
            }
        )
    }

    if (showAlert) {
        AlertDialog(
            onDismissRequest = {
                jsAlertResult?.cancel()
                showAlert = false
            },
            title = { Text("알림") },
            text = { Text(alertMessage ?: "") },
            confirmButton = {
                TextButton(onClick = {
                    jsAlertResult?.confirm()
                    showAlert = false
                }) {
                    Text("확인")
                }
            }
        )
    }

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
            )
        }
    ) { innerPadding ->
        Box(modifier = modifier.padding(innerPadding).fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    SwipeRefreshLayout(ctx).apply {
                        setOnRefreshListener {
                            isRefreshing = true
                            webView.reload()
                        }
                        addView(webView, LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.MATCH_PARENT
                        ))

                        webView.webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                isRefreshing = false
                                isRefreshing = false
                                this@apply.isRefreshing = false
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val url = request?.url.toString()
                                return handleCustomUrl(ctx, view, url)
                            }
                        }

                        webView.webChromeClient = object : WebChromeClient() {
                            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                                alertMessage = message
                                jsAlertResult = result
                                showAlert = true
                                return true
                            }

                            override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
                                val newWebView = WebView(view!!.context)
                                val transport = resultMsg?.obj as? WebView.WebViewTransport
                                transport?.webView = newWebView
                                resultMsg?.sendToTarget()
                                newWebView.webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(wv: WebView, request: WebResourceRequest?): Boolean {
                                        val newUrl = request?.url.toString()
                                        if (isSameDomain(view.url, newUrl)) {
                                            view.loadUrl(newUrl)
                                        } else {
                                            popupUrl = newUrl
                                            webViewForPopup = view
                                            showUrlPopup = true
                                        }
                                        return true
                                    }
                                }
                                return true
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private fun handleCustomUrl(context: android.content.Context, view: WebView?, url: String): Boolean {
    return try {
        if (url.startsWith("intent:") ||
            url.contains("ispmobile://") ||
            url.startsWith("market://") ||
            url.contains("vguard") ||
            url.contains("droidxantivirus") ||
            url.contains("v3mobile") ||
            url.endsWith(".apk") ||
            url.contains("mvaccine") ||
            url.startsWith("smartwall://") ||
            url.startsWith("nidlogin://") ||
            url == "http://m.ahnlab.com/kr/site/download"
        ) {
            val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
            val packageName = intent.`package`
            var storeUrl: String? = null

            when {
                url.contains("ispmobile") -> {
                    storeUrl = "http://mobile.vpay.co.kr/jsp/MISP/andown.jsp"
                }
                url.startsWith("tmap:") -> {
                    storeUrl = "https://play.google.com/store/apps/details?id=com.skt.tmap.ku"
                }
                url.startsWith("intent:kakaonavi-sdk") -> {
                    storeUrl = "https://play.google.com/store/apps/details?id=com.locnall.KimGiSa"
                }
                url.contains("kakaolink") || url.contains("plusfriend") -> {
                    storeUrl = "https://play.google.com/store/apps/details?id=com.kakao.talk"
                }
                else -> {
                    if (!packageName.isNullOrEmpty()) {
                        storeUrl = "market://details?id=$packageName"
                    }
                }
            }

            if (isPackageInstalled(packageName, context.packageManager)) {
                context.startActivity(intent)
            } else {
                storeUrl?.let {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
                }
            }
            true
        } else {
            if (shouldOpenInExternalBrowser(view?.url, url)) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                true
            } else {
                false
            }
        }
    } catch (e: Exception) {
        true
    }
}

private fun isPackageInstalled(packageName: String?, packageManager: PackageManager): Boolean {
    if (packageName.isNullOrEmpty()) return false
    return try {
        packageManager.getApplicationInfo(packageName, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}

private fun isSameDomain(currentUrl: String?, newUrl: String?): Boolean {
    return try {
        val currentUri = URI(currentUrl ?: "")
        val newUri = URI(newUrl ?: "")
        currentUri.host.equals(newUri.host, ignoreCase = true)
    } catch (_: Exception) {
        false
    }
}

private fun shouldOpenInExternalBrowser(currentWebViewUrl: String?, targetUrl: String?): Boolean {
    return try {
        val currentUri = URI(currentWebViewUrl ?: "")
        val targetUri = URI(targetUrl ?: "")
        currentUri.host != targetUri.host
    } catch (_: Exception) {
        true
    }
}
