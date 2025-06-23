package com.peachspot.legendkofarm.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Message
import android.provider.MediaStore
import android.util.Log
import android.webkit.*
import android.widget.LinearLayout
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.gms.common.wrappers.Wrappers.packageManager
import java.io.File
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Context
import android.os.Environment
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

object DiaryScreenContentTypes {
    const val INFO = "info"
    const val SUCCESS = "success"
    const val ERROR = "error"
}

private var fileCallback: ValueCallback<Array<Uri>>? = null
private var imageUri: Uri? = null

private fun handleCustomUrl(context: Context, view: WebView?, url: String): Boolean {
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
    } catch (_: Exception) {
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



@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CommonWebView(
    webView: WebView,
    modifier: Modifier = Modifier,
    onShowPopup: (String, WebView) -> Unit,
    onJsAlert: (String, JsResult) -> Unit,
    onFileChooserRequest: (ValueCallback<Array<Uri>>, Intent) -> Unit
) {
    val context = LocalContext.current
    var isRefreshing by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    webView.onPause()
                    webView.pauseTimers()
                }
                Lifecycle.Event.ON_RESUME -> {
                    webView.resumeTimers()
                    webView.onResume()

                    val currentUrl = webView.url
                    if (currentUrl.isNullOrBlank() || currentUrl == "about:blank") {
                        // URL이 비었을 경우에만 다시 로딩 (원래 URL을 기억하려면 ViewModel에서 관리)
                    }
                }
                Lifecycle.Event.ON_DESTROY -> {
                    webView.removeJavascriptInterface("Android")
                    webView.destroy()
                }
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AndroidView(
        factory = {
            SwipeRefreshLayout(context).apply {
                isEnabled = false

                setOnRefreshListener {
                    isRefreshing = true
                    webView.reload()
                }

                addView(
                    webView.apply {
                        (parent as? ViewGroup)?.removeView(this)
                    },
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                    )
                )


//                addView(
//                    webView,
//                    LinearLayout.LayoutParams(
//                        LinearLayout.LayoutParams.MATCH_PARENT,
//                        LinearLayout.LayoutParams.MATCH_PARENT
//                    )
//                )

                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        isRefreshing = false
                        this@apply.isRefreshing = false

                        view?.evaluateJavascript(
                            "(function() { console.log('[WEBVIEW] Loaded URL: ' + window.location.href); })();",
                            null
                        )
                    }



                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val url = request?.url.toString()
                        return handleCustomUrl(context, view, url)
                    }
                }

                webView.webChromeClient = object : WebChromeClient() {
                    override fun onShowFileChooser(
                        webView: WebView?,
                        filePathCallback: ValueCallback<Array<Uri>>,
                        fileChooserParams: FileChooserParams
                    ): Boolean {
                        val intent = fileChooserParams.createIntent()
                        onFileChooserRequest(filePathCallback, intent)
                        return true
                    }

                    override fun onJsAlert(
                        view: WebView?,
                        url: String?,
                        message: String?,
                        result: JsResult?
                    ): Boolean {
                        return if (message != null && result != null) {
                            onJsAlert(message, result)
                            result.confirm()
                            true
                        } else {
                            false
                        }
                    }

                    override fun onCreateWindow(
                        view: WebView?,
                        isDialog: Boolean,
                        isUserGesture: Boolean,
                        resultMsg: Message?
                    ): Boolean {
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
                                    onShowPopup(newUrl, view)
                                }
                                return true
                            }
                        }
                        return true
                    }

                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        consoleMessage?.let {
                            Log.d(
                                "WebViewConsole",
                                "[${it.messageLevel()}] ${it.message()} -- ${it.sourceId()}:${it.lineNumber()}"
                            )
                        }
                        return super.onConsoleMessage(consoleMessage)
                    }
                }
            }
        },
        modifier = modifier
    )
}



@Composable
fun CommonWebView_02(
    url: String,
    modifier: Modifier = Modifier,
    onShowPopup: (String, WebView) -> Unit,
    onJsAlert: (String, JsResult) -> Unit,
    onFileChooserRequest: (ValueCallback<Array<Uri>>, Intent) -> Unit
) {
    val context = LocalContext.current
    var isRefreshing by remember { mutableStateOf(false) }

    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.setSupportMultipleWindows(false)
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            loadUrl(url)
        }
    }

    // ✅ Lifecycle 대응 추가
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    webView.onPause()
                    webView.pauseTimers()
                }
                Lifecycle.Event.ON_RESUME -> {
                    webView.resumeTimers()
                    webView.onResume()

                    // ✅ 포그라운드 복귀 시 WebView 상태 확인 후 reload
                    val currentUrl = webView.url
                    if (currentUrl.isNullOrBlank() || currentUrl == "about:blank") {
                        webView.loadUrl(url)
                    }
                }
                Lifecycle.Event.ON_DESTROY -> {
                    webView.removeJavascriptInterface("Android")
                    webView.destroy()
                }
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }


    AndroidView(
        factory = { ctx ->
            SwipeRefreshLayout(ctx).apply {
                isEnabled = false

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
                        this@apply.isRefreshing = false

                        view?.evaluateJavascript(
                            "(function() { console.log('[WEBVIEW] Loaded URL: ' + window.location.href); })();",
                            null
                        )
                    }

                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val url = request?.url.toString()
                        return handleCustomUrl(ctx, view, url)
                    }
                }

                webView.webChromeClient = object : WebChromeClient() {
                    override fun onShowFileChooser(
                        webView: WebView?,
                        filePathCallback: ValueCallback<Array<Uri>>,
                        fileChooserParams: FileChooserParams
                    ): Boolean {
                        val intent = fileChooserParams.createIntent()
                        onFileChooserRequest(filePathCallback, intent)
                        return true
                    }

                    override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                        return if (message != null && result != null) {
                            onJsAlert(message, result)
                            result.confirm()
                            true
                        } else {
                            false
                        }
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
                                    onShowPopup(newUrl, view)
                                }
                                return true
                            }
                        }
                        return true
                    }

                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        consoleMessage?.let {
                            Log.d("WebViewConsole", "[${it.messageLevel()}] ${it.message()} -- ${it.sourceId()}:${it.lineNumber()}")
                        }
                        return super.onConsoleMessage(consoleMessage)
                    }
                }
            }
        },
        modifier = modifier
    )




}
