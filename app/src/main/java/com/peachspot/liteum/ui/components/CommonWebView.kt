package com.peachspot.liteum.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Message
import android.util.Log
import android.webkit.*
import android.widget.LinearLayout
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import java.net.URI
import android.content.Context
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.peachspot.liteum.util.Logger

// com.google.android.gms.common.wrappers.Wrappers.packageManager 는 제거했습니다.
// 안드로이드 기본 packageManager를 사용합니다.

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
            Logger.d("nicap AAAAAAAAAAAA", "inside handdle");
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
            // 이 부분은 기존 로직을 유지합니다.
            // shouldOverrideUrlLoading에서 이 함수의 반환 값을 기반으로 추가 로드 여부를 결정합니다.
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
    webView: WebView, // 외부에서 주입받는 WebView 인스턴스를 사용합니다.
    modifier: Modifier = Modifier,
    onShowPopup: (String, WebView) -> Unit,
    onJsAlert: (String, JsResult) -> Unit,
    onFileChooserRequest: (ValueCallback<Array<Uri>>, Intent) -> Unit
) {
    val context = LocalContext.current
    var isRefreshing by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, webView) { // webView를 key로 추가하여 라이프사이클에 따라 적절히 동작하도록 합니다.
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    webView.onPause()
                    webView.pauseTimers()
                }

                Lifecycle.Event.ON_RESUME -> {
                    webView.resumeTimers()
                    webView.onResume()

                    // 기존 로직 유지
                    val currentUrl = webView.url
                    if (currentUrl.isNullOrBlank() || currentUrl == "about:blank") {
                        // URL이 비었을 경우에만 다시 로딩 (원래 URL을 기억하려면 ViewModel에서 관리)
                    }
                }

                Lifecycle.Event.ON_DESTROY -> {
                    // webView.removeJavascriptInterface("Android") // 필요한 경우에만 유지
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
        modifier = modifier, // modifier를 SwipeRefreshLayout에 적용합니다.
        factory = {
            SwipeRefreshLayout(context).apply {
                isEnabled = false // 초기에는 새로고침 비활성화

                // 외부에서 주입받은 webView 인스턴스를 여기에서 설정하고 사용합니다.
                // WebView를 이 곳에서 새로 생성하지 않습니다.
                webView.apply {
                    // 부모 뷰가 있다면 제거합니다. (Composable에서 WebView 인스턴스 재사용 시 필요)
                    (parent as? ViewGroup)?.removeView(this)

                    // 줌인/줌아웃 허용 설정
                    settings.setSupportZoom(true)           // 줌 기능 지원 활성화
                    settings.setBuiltInZoomControls(true)   // 내장 줌 컨트롤 활성화 (제스처 줌 허용을 위해 필요)
                    settings.displayZoomControls = false    // 화면에 줌 컨트롤 버튼 표시 여부 (false로 하면 버튼은 안 보이지만 제스처 줌은 가능)
                }

                // SwipeRefreshLayout에 WebView를 추가합니다.
                addView(
                    webView,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                    )
                )

                // SwipeRefreshLayout의 리스너는 주입받은 webView 인스턴스를 사용합니다.
                setOnRefreshListener {
                    isRefreshing = true
                    webView.reload()
                }

                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        isRefreshing = false
                        this@apply.isRefreshing = false // SwipeRefreshLayout의 isRefreshing 상태 업데이트
                        CommonWebViewState.lastKnownUrl = url
                        view?.evaluateJavascript(
                            "(function() { console.log('[WEBVIEW] Loaded URL: ' + window.location.href); })();",
                            null
                        )
                    }

                    override fun shouldOverrideUrlLoading(
                        wv: WebView,
                        req: WebResourceRequest
                    ): Boolean {
                        val url = req.url.toString()
                        val baseUrl = wv.url ?: CommonWebViewState.lastKnownUrl ?: ""

                        return when {
                            // 같은 도메인일 경우 WebView가 직접 로드
                            isSameDomain(baseUrl, url) -> {
                                wv.loadUrl(url)
                                true
                            }
                            // 외부 앱/스토어/특정 프로토콜 처리
                            handleCustomUrl(context, wv, url) -> {
                                // handleCustomUrl 내에서 이미 처리했으므로 true 반환
                                true
                            }
                            // 다른 도메인이고 handleCustomUrl에서 처리하지 않았다면 외부 브라우저로 열지 말지 결정
                            shouldOpenInExternalBrowser(baseUrl, url) -> {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                true
                            }
                            // 위 조건들에 해당하지 않으면 WebView가 직접 로드하도록 false 반환
                            else -> false
                        }
                    }

                    // 오류 핸들링 오버라이드 추가
                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        Log.e("WebViewError", "Error loading ${request?.url}: ${error?.description}")
                        // 여기에 오류 페이지 로딩 또는 사용자에게 알림 로직 추가
                    }

                    override fun onReceivedHttpError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        errorResponse: WebResourceResponse?
                    ) {
                        super.onReceivedHttpError(view, request, errorResponse)
                        Log.e("WebViewHttpError", "HTTP Error loading ${request?.url}: ${errorResponse?.statusCode}")
                    }
                }

                webView.webChromeClient = object : WebChromeClient() {
                    override fun onShowFileChooser(
                        webView: WebView?,
                        filePathCallback: ValueCallback<Array<Uri>>,
                        fileChooserParams: FileChooserParams
                    ): Boolean {
                        fileCallback = filePathCallback // 전역 변수에 저장
                        onFileChooserRequest(filePathCallback, fileChooserParams.createIntent())
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
                            result.confirm() // confirm을 호출하여 JS alert를 닫음
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
                        val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
                        val newWebView = WebView(context).apply { // 새 WebView 인스턴스 생성
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.javaScriptCanOpenWindowsAutomatically = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.cacheMode = WebSettings.LOAD_NO_CACHE
                            isFocusable = true
                            isFocusableInTouchMode = true
                            requestFocus()
                            // 줌인/줌아웃 허용 설정 (새 창에도 동일하게 적용)
                            settings.setSupportZoom(true)
                            settings.setBuiltInZoomControls(true)
                            settings.displayZoomControls = false

                            // 새 창도 멀티윈도우 지원
                            settings.setSupportMultipleWindows(true)

                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    wv: WebView,
                                    req: WebResourceRequest
                                ): Boolean {
                                    val url = req.url.toString()
                                    val mainWebView = view ?: return false // 원래 호출한 WebView
                                    val baseUrl = mainWebView.url ?: CommonWebViewState.lastKnownUrl ?: ""

                                    // 1. 같은 도메인 → 메인 WebView에서 열고 새 WebView는 닫기
                                    if (isSameDomain(baseUrl, url)) {
                                        mainWebView.loadUrl(url)
                                        wv.destroy() // 새 WebView 닫기
                                        return true
                                    }

                                    // 2. 외부 URL → handleCustomUrl로 처리 (메인 WebView 컨텍스트 전달)
                                    if (handleCustomUrl(context, mainWebView, url)) {
                                        wv.destroy() // 새 WebView 닫기
                                        return true
                                    }

                                    // 3. 다른 도메인 → 새 창 WebView 그대로 사용 (팝업으로 표시)
                                    // onShowPopup 콜백을 통해 UI 계층에 추가하도록 합니다.
                                    onShowPopup(url, wv) // url과 childWebView를 넘겨서 팝업을 표시하도록 요청
                                    return true // shouldOverrideUrlLoading에서 처리했으므로 true 반환
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onCloseWindow(window: WebView?) {
                                    super.onCloseWindow(window)
                                    // 새 창 닫기 로직 (UI에서 팝업 WebView를 제거해야 함)
                                    window?.destroy()
                                }
                            }
                        }

                        transport.webView = newWebView
                        resultMsg.sendToTarget()

                        return true
                    }


                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        consoleMessage?.let {
                            Logger.d(
                                "WebViewConsole",
                                "[${it.messageLevel()}] ${it.message()} -- ${it.sourceId()}:${it.lineNumber()}"
                            )
                        }
                        return super.onConsoleMessage(consoleMessage)
                    }
                }
            }
        },
        update = { swipeRefreshLayout ->
            // isRefreshing 상태 업데이트
            swipeRefreshLayout.isRefreshing = isRefreshing
        }
    )
}
