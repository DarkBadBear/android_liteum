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
import android.graphics.Bitmap
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

object CommonWebViewState {
    var lastKnownUrl: String? = null
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
            Log.d("nicap AAAAAAAAAAAA", "inside handdle");
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
                view?.loadUrl(url) // 이 한 줄이 있어야 내부 WebView가 처리함!
                true
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


fun isSameDomain(url1: String?, url2: String?): Boolean {
    return try {
        val host1 = Uri.parse(url1).host?.removePrefix("www.")?.lowercase()
        val host2 = Uri.parse(url2).host?.removePrefix("www.")?.lowercase()
        host1 != null && host1 == host2
    } catch (e: Exception) {
        false
    }
}


private fun shouldOpenInExternalBrowser(currentWebViewUrl: String?, targetUrl: String?): Boolean {
    return try {
        val currentUri = URI(currentWebViewUrl ?: "")
        val targetUri = URI(targetUrl ?: "")

        val currentHost = currentUri.host ?: ""
        val targetHost = targetUri.host ?: ""

        if (currentHost.isEmpty() || targetHost.isEmpty()) {
            // 호스트가 없으면 외부 브라우저로 열자 (또는 내부 처리 원하면 false로)
            true
        } else {
            currentHost != targetHost
        }
    } catch (e: Exception) {
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

                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.javaScriptCanOpenWindowsAutomatically = true
                    settings.setSupportMultipleWindows(true)
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.cacheMode = WebSettings.LOAD_NO_CACHE
                }


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

                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        isRefreshing = false
                        this@apply.isRefreshing = false
                        CommonWebViewState.lastKnownUrl = url
                        view?.evaluateJavascript(
                            "(function() { console.log('[WEBVIEW] Loaded URL: ' + window.location.href); })();",
                            null
                        )
                    }

//                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
//                        val url = request?.url.toString()
//                        return handleCustomUrl(context, view, url)
//                    }

                    override fun shouldOverrideUrlLoading(
                        wv: WebView,
                        req: WebResourceRequest
                    ): Boolean {
                        val url = req.url.toString()
                        val baseUrl = wv.url ?: CommonWebViewState.lastKnownUrl ?: ""

                        return when {

                            isSameDomain(baseUrl, url) -> {
                                wv.loadUrl(url)
                                true
                            }

                            // 1. 외부 URL → handleCustomUrl로 처리
                            handleCustomUrl(context, wv, url) -> {
                                wv.post {
                                }
                                true
                            }

                            // 2. 같은 도메인 → 기존 WebView에서 열기


                            // 3. 다른 도메인 → 새창 유지 (childWebView 그대로)
                            else -> false
                        }

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
                        val context = view?.context ?: return false
                        val mainWebView = view ?: return false

                        val childWebView = WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.javaScriptCanOpenWindowsAutomatically = true
                            settings.domStorageEnabled = true
                            settings.setSupportMultipleWindows(false)

                            webViewClient = object : WebViewClient() {

                                override fun shouldOverrideUrlLoading(
                                    wv: WebView,
                                    req: WebResourceRequest
                                ): Boolean {
                                    val url = req.url.toString()
                                    val baseUrl =
                                        mainWebView.url ?: CommonWebViewState.lastKnownUrl ?: ""

                                    return when {
                                        // 1. 같은 도메인 → 기존 WebView에서 열기
                                        isSameDomain(baseUrl, url) -> {
                                            mainWebView.loadUrl(url)
                                            wv.destroy()
                                            true
                                        }

                                        // 2. 외부 URL → handleCustomUrl로 처리
                                        handleCustomUrl(context, mainWebView, url) -> {
                                            wv.post {
                                                wv.destroy()
                                            }
                                            true
                                        }


                                        // 3. 다른 도메인 → 새창 유지 (childWebView 그대로)
                                        else -> false
                                    }

                                }
                            }

                            webChromeClient = object : WebChromeClient() {} // 내부 팝업 방지용
                        }

                        val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
                        transport.webView = childWebView
                        resultMsg.sendToTarget()

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


