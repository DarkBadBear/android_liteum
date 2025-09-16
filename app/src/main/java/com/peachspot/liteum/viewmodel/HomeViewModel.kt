package com.peachspot.liteum.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.IntentSender
import android.webkit.WebView
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.peachspot.liteum.data.remote.api.MyApiService
import com.peachspot.liteum.data.repositiory.HomeRepository
import com.peachspot.liteum.data.repositiory.UserPreferencesRepository
import com.peachspot.liteum.data.repositiory.UserProfileData
import com.peachspot.liteum.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.peachspot.liteum.R
import com.peachspot.liteum.data.remote.client.NetworkClient
import kotlinx.coroutines.delay
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebViewClient
import androidx.credentials.exceptions.NoCredentialException
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.kakao.sdk.user.UserApiClient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Application.dataStore by preferencesDataStore("secure_prefs")

data class AuthUiState(
    val isUserLoggedIn: Boolean = false,
    val isLoading: Boolean = true,
    val isEnding: Boolean = false,
    val userMessage: String? = null,
    val userMessageType: String? = null,
    val signInPendingIntent: IntentSender? = null,
    val firebaseUid: String? = null,
    val kakaoUid: String? = null,
    val userName: String? = null,
    val userEmail: String? = null,
    val userPhotoUrl: String? = null,
    val requiresReAuthentication: Boolean = false,
    val webViewAuthUrl: String? = null
)

class HomeViewModel (
    private val application: Application,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val firebaseAuth: FirebaseAuth,
    private val credentialManager: CredentialManager,
    private val myApiService: MyApiService,
    private val homeRepository: HomeRepository,
): ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    private val _loginResult = MutableStateFlow<Result<String>?>(null)
    val loginResult: StateFlow<Result<String>?> = _loginResult

    private var activeWebView: WebView? = null

    // WebView 캐시 관리
    private val webViewMap = mutableMapOf<String, WebView>()
    private val webViewRefreshTrigger = MutableStateFlow(false)
// HomeViewModel.kt
//
//    fun getOrCreateWebView(context: Context, tag: String, url: String): WebView {
//        return webViewMap.getOrPut(tag) {
//            createWebView(context, url)
//        }.also { webView ->
//            // 백그라운드에서 돌아온 후 새로고침 필요할 때만 reload
//            if (webViewRefreshTrigger.value) {
//                Logger.d("HomeViewModel", "Refreshing WebView after background return: $tag")
//                loadUrlWithHeaders(webView, url, emptyMap())
//            }
//        }
//    }

    fun getOrCreateWebView(context: Context, tag: String, url: String): WebView {
        val webViewInstance = webViewMap.getOrPut(tag) {
            Logger.d("HomeViewModel", "Creating new WebView for tag: $tag, url: $url")
            createWebView(context, url) // createWebView는 초기 URL로 바로 로드합니다.
        }.also { webView ->
            // URL이 변경되었거나, 새로고침 트리거가 활성화된 경우에만 URL을 다시 로드합니다.
            // 주의: 이 로직은 webView가 재사용될 때 이전 URL과 다른 URL로 업데이트해야 하는 경우 중요합니다.
            // 만약 동일 태그에 항상 동일 URL이거나, URL 변경 시 새 태그로 WebView를 만든다면 이 조건은 단순화될 수 있습니다.
            val currentUrl = webView.url
            if (currentUrl != url || webViewRefreshTrigger.value) {
                Logger.d("HomeViewModel", "Loading/Refreshing WebView for tag: $tag. New URL: $url. Current URL: $currentUrl. RefreshTrigger: ${webViewRefreshTrigger.value}")
                loadUrlWithHeaders(webView, url, emptyMap())
            }
        }
        // 이 WebView를 현재 활성화된 WebView로 설정합니다.
        // 만약 여러 WebView 중 하나를 선택적으로 활성화해야 한다면,
        // 이 로직은 해당 WebView가 실제로 화면에 표시되는 시점에 호출되도록 조정해야 할 수 있습니다.
        this.activeWebView = webViewInstance
        Logger.d("HomeViewModel", "Active WebView set to: $tag")
        return webViewInstance
    }

    // TopBar의 확대 버튼 클릭 시 호출될 함수
    fun zoomInActiveWebView() {
        activeWebView?.let {
            if (it.canZoomIn()) {
                it.zoomIn()
                Logger.d("HomeViewModel", "Zoom In called on active WebView.")
            } else {
                Logger.d("HomeViewModel", "Cannot Zoom In further on active WebView.")
            }
        } ?: Logger.w("HomeViewModel", "Zoom In called but no active WebView.")
    }

    // TopBar의 축소 버튼 클릭 시 호출될 함수
    fun zoomOutActiveWebView() {
        activeWebView?.let {
            if (it.canZoomOut()) {
                it.zoomOut()
                Logger.d("HomeViewModel", "Zoom Out called on active WebView.")
            } else {
                Logger.d("HomeViewModel", "Cannot Zoom Out further on active WebView.")
            }
        } ?: Logger.w("HomeViewModel", "Zoom Out called but no active WebView.")
    }

    // createWebView는 initialUrl을 받을 수 있도록 수정 (이전 답변 참고)
    private fun createWebView(context: Context, initialUrl: String?): WebView {
        return WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                javaScriptCanOpenWindowsAutomatically = true
                setSupportMultipleWindows(false)
                loadWithOverviewMode = true
                useWideViewPort = true
                allowFileAccess = true
                allowContentAccess = true
                cacheMode = WebSettings.LOAD_DEFAULT
            }

            isFocusable = true
            isFocusableInTouchMode = true
            requestFocus()

            webViewClient = object : WebViewClient() {
                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    Logger.e("WebViewLoad", "Error loading URL: ${request?.url}, Description: ${error?.description}, Code: ${error?.errorCode}")
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Logger.d("WebViewLoad", "Page finished loading: $url")
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    Logger.d("WebViewProgress", "Loading progress: $newProgress%")
                }
            }

            if (!initialUrl.isNullOrBlank()) {
                loadUrl(initialUrl) // createWebView 시점에 initialUrl이 있으면 로드
            }
        }
    }


    fun loadUrlWithHeaders(webView: WebView, url: String, headers: Map<String, String>) {
        try {
            webView.post {
                if (headers.isNotEmpty()) {
                    webView.loadUrl(url, headers)
                } else {
                    webView.loadUrl(url)
                }
            }
        } catch (e: Exception) {
            Logger.e("HomeViewModel", "loadUrlWithHeaders failed", e)
        }
    }

    // 백그라운드에서 돌아온 후 웹뷰 새로고침
    fun refreshWebViewsAfterBackground() {
        Logger.d("HomeViewModel", "Triggering WebView refresh after background return")
        webViewRefreshTrigger.value = true

        // 일정 시간 후 플래그 리셋 (다음 번 새로고침을 위해)
        viewModelScope.launch {
            delay(1000) // 1초 후 리셋
            webViewRefreshTrigger.value = false
        }
    }

    // WebView 메모리 정리 (필요시)
    fun clearWebViewCache() {
        webViewMap.forEach { (tag, webView) ->
            try {
                webView.clearCache(true)
                webView.clearHistory()
                Logger.d("HomeViewModel", "Cleared cache for WebView: $tag")
            } catch (e: Exception) {
                Logger.e("HomeViewModel", "Failed to clear cache for WebView: $tag", e)
            }
        }
    }

    // WebView 완전 제거 (메모리 해제)
    fun destroyWebViews() {
        webViewMap.forEach { (tag, webView) ->
            try {
                webView.removeAllViews()
                webView.destroy()
                Logger.d("HomeViewModel", "Destroyed WebView: $tag")
            } catch (e: Exception) {
                Logger.e("HomeViewModel", "Failed to destroy WebView: $tag", e)
            }
        }
        webViewMap.clear()
    }

    override fun onCleared() {
        super.onCleared()
        activeWebView = null
        destroyWebViews()
    }

    // ---------------------- Google 로그인 ----------------------
    private val googleIdOption: GetGoogleIdOption by lazy {
        GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(application.getString(R.string.default_web_client_id))
            .setAutoSelectEnabled(false)
            .build()
    }

    // ---------------------- Tink 암호화 초기화 (카카오용) ----------------------
    private val tokenKey = stringPreferencesKey("kakao_id_token")
    private val aead: Aead by lazy {
        AeadConfig.register()
        val keysetHandle: KeysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(application, "master_keyset", "master_prefs")
            .withKeyTemplate(com.google.crypto.tink.aead.AesGcmKeyManager.aes256GcmTemplate())
            .withMasterKeyUri("android-keystore://master_key")
            .build()
            .keysetHandle

        keysetHandle.getPrimitive(Aead::class.java)
    }

    init {
        viewModelScope.launch {
            checkCurrentUser()
        }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    // ---------------------- 사용자 로그인 상태 체크 ----------------------
    fun checkCurrentUser() {
        viewModelScope.launch {
            var loggedIn = false
            var firebaseUid: String? = null
            var kakaoUid: String? = null
            var userName: String? = null
            var userEmail: String? = null
            var userPhotoUrl: String? = null

            // Firebase 로그인
            firebaseAuth.currentUser?.let { user ->
                firebaseUid = user.uid
                userName = user.displayName
                userEmail = user.email
                userPhotoUrl = user.photoUrl?.toString()
                loggedIn = true
                Logger.d("AuthViewModel", "Firebase user logged in")
            }

            // 카카오 로그인 (Firebase 로그인 없으면)
            if (!loggedIn) {
                val kakaoToken = loadKakaoIdToken()
                if (!kakaoToken.isNullOrEmpty()) {
                    // 카카오 사용자 정보 가져오기
                    UserApiClient.instance.me { user, error ->
                        if (user != null) {
                            kakaoUid = user.id.toString()
                            userName = user.kakaoAccount?.profile?.nickname
                            userEmail = user.kakaoAccount?.email
                            userPhotoUrl = user.kakaoAccount?.profile?.thumbnailImageUrl
                            loggedIn = true
                            Logger.d("AuthViewModel", "Kakao user logged in: $kakaoUid")
                            _uiState.update {
                                it.copy(
                                    isUserLoggedIn = loggedIn,
                                    kakaoUid = kakaoUid,
                                    userName = userName,
                                    userEmail = userEmail,
                                    userPhotoUrl = userPhotoUrl,
                                    isLoading = false,
                                    webViewAuthUrl = if (loggedIn) "https://peachspot.co.kr/lkfAuth" else null
                                )
                            }
                        }
                    }
                }
            }

            if (loggedIn) {
                _uiState.update {
                    it.copy(
                        isUserLoggedIn = loggedIn,
                        firebaseUid = firebaseUid,
                        isLoading = false,
                        webViewAuthUrl = "https://peachspot.co.kr/lkfAuth"
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // ---------------------- 카카오 로그인 ----------------------
    fun startKakaoSignIn(activity: Activity) {
        if (UserApiClient.instance.isKakaoTalkLoginAvailable(activity)) {
            UserApiClient.instance.loginWithKakaoTalk(activity) { token, error ->
                handleKakaoLoginResult(token?.idToken, error)
            }
        } else {
            UserApiClient.instance.loginWithKakaoAccount(activity) { token, error ->
                handleKakaoLoginResult(token?.idToken, error)
            }
        }
    }

    private fun handleKakaoLoginResult(idToken: String?, error: Throwable?) {
        if (error != null) {
            _loginResult.value = Result.failure(error)
            _uiState.update { it.copy(isLoading = false, userMessage = error.localizedMessage) }
        } else if (idToken != null) {
            viewModelScope.launch { saveKakaoIdToken(idToken) }

            // 카카오 사용자 정보 가져오기
            UserApiClient.instance.me { user, err ->
                val kakaoUid = user?.id?.toString()
                val userName = user?.kakaoAccount?.profile?.nickname
                val userEmail = user?.kakaoAccount?.email
                val userPhotoUrl = user?.kakaoAccount?.profile?.thumbnailImageUrl

                _loginResult.value = Result.success(idToken)
                _uiState.update {
                    it.copy(
                        isUserLoggedIn = true,
                        kakaoUid = kakaoUid,
                        userName = userName,
                        userEmail = userEmail,
                        userPhotoUrl = userPhotoUrl,
                        isLoading = false,
                        webViewAuthUrl = "https://peachspot.co.kr/lkfAuth"
                    )
                }
            }
        }
    }
    // TopBar의 확대 버튼 클릭 시 호출될 함수

    private suspend fun saveKakaoIdToken(token: String) {
        val encrypted = aead.encrypt(token.toByteArray(), null)
        application.dataStore.edit { prefs ->
            prefs[tokenKey] = android.util.Base64.encodeToString(encrypted, android.util.Base64.DEFAULT)
        }
    }

    private suspend fun loadKakaoIdToken(): String? {
        val base64 = application.dataStore.data.map { it[tokenKey] }.first()
        return base64?.let {
            val decrypted = aead.decrypt(android.util.Base64.decode(it, android.util.Base64.DEFAULT), null)
            String(decrypted)
        }
    }

    private suspend fun clearKakaoIdToken() {
        application.dataStore.edit { it.remove(tokenKey) }
    }

    // ---------------------- Google 로그인 ----------------------
    fun startGoogleSignIn() {
        _uiState.value = _uiState.value.copy(isLoading = true, userMessage = null, signInPendingIntent = null)
        viewModelScope.launch {
            try {
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result: GetCredentialResponse =
                    credentialManager.getCredential(request = request, context = application)
                handleSignInCredential(result.credential)

            } catch (e: NoCredentialException) {
                Logger.d("AuthViewModel", "No saved credentials")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    userMessage = "저장된 계정이 없습니다.",
                    userMessageType = "info"
                )
            } catch (e: GetCredentialException) {
                Logger.e("AuthViewModel", "GetCredentialException", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    userMessage = "로그인 실패, 다시 시도해 주세요.",
                    userMessageType = "error"
                )
            } catch (e: Exception) {
                Logger.e("AuthViewModel", "General Exception", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    userMessage = "로그인 중 오류가 발생했습니다."
                )
            }
        }
    }

    private suspend fun handleSignInCredential(credential: androidx.credentials.Credential) {
        try {
            if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val googleIdToken = googleIdTokenCredential.idToken
                firebaseAuthWithGoogleToken(googleIdToken)


            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    userMessage = "지원되지 않는 인증 유형입니다."
                )
            }
        } catch (e: GoogleIdTokenParsingException) {
            Logger.e("AuthViewModel", "GoogleIdTokenParsingException", e)
            _uiState.value = _uiState.value.copy(isLoading = false, userMessage = "토큰 파싱 실패")
        }
    }

    private suspend fun firebaseAuthWithGoogleToken(idToken: String) {
        val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
        try {
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val user = authResult.user ?: throw Exception("Firebase User is null")

            val emailFromToken = parseEmailFromIdToken(idToken)

            val userProfile = UserProfileData(firebaseUid = user.uid, googleId = null,email=emailFromToken)
            userPreferencesRepository.saveUserProfileData(userProfile)

            _uiState.value = _uiState.value.copy(
                isUserLoggedIn = true,
                isLoading = false,
                firebaseUid = user.uid,
                userEmail = emailFromToken,
                userMessage = "로그인 되었습니다.",
                webViewAuthUrl = "https://peachspot.co.kr/lkfAuth"
            )

            val fcmToken = FirebaseMessaging.getInstance().token.await()
            val uid = firebaseAuth.currentUser?.uid ?: ""
            if (!fcmToken.isNullOrBlank()) {
                NetworkClient.myApiService.registerUser(uid, fcmToken)
            }
        } catch (e: Exception) {
            Logger.e("AuthViewModel", "Firebase Auth failed", e)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                userMessage = "로그인 실패: ${e.localizedMessage}",
                isUserLoggedIn = false,
                firebaseUid = null,
                userEmail = null,
                webViewAuthUrl = null
            )
        }
    }

    fun parseEmailFromIdToken(idToken: String): String? {
        val parts = idToken.split(".")
        if (parts.size == 3) {
            val payload = String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE))
            val json = org.json.JSONObject(payload)
            return json.optString("email", null)
        }
        return null
    }


    // ---------------------- 로그아웃 ----------------------
    fun logOut() {
        _uiState.value = _uiState.value.copy(isEnding = true)
        viewModelScope.launch {
            try {
                // 1. Firebase 로그아웃
                firebaseAuth.signOut()

                // 2. Google CredentialManager 로그아웃
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Logger.w("AuthViewModel", "CredentialManager clear failed", e)
                }

                // 3. 카카오 SDK 로그아웃 + 토큰 삭제
                try {
                    UserApiClient.instance.logout { error ->
                        if (error != null) Logger.e("AuthViewModel", "Kakao logout failed", error)
                        else Logger.d("AuthViewModel", "Kakao logout success")
                    }
                    clearKakaoIdToken()
                } catch (e: Exception) {
                    Logger.w("AuthViewModel", "Kakao logout/token clear failed", e)
                }

                // 4. WebView 캐시 정리
                clearWebViewCache()

                // 5. UI 상태 초기화
                _uiState.value = AuthUiState(
                    isUserLoggedIn = false,
                    isLoading = false,
                    isEnding = false,
                    userMessage = "로그아웃 되었습니다."
                )
            } catch (e: Exception) {
                Logger.e("AuthViewModel", "LogOut failed", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isEnding = false,
                    userMessage = "로그아웃 실패: ${e.localizedMessage}"
                )
            }
        }
    }

    // ---------------------- 탈퇴 ----------------------
    fun signOut() {
        _uiState.value = _uiState.value.copy(isEnding = true)

        viewModelScope.launch {
            try {
                // 1. Firebase 계정 삭제 및 로그아웃
                firebaseAuth.currentUser?.let { user ->
                    try {
                        user.delete().await()
                        Logger.d("AuthViewModel", "Firebase user account deleted successfully.")
                    } catch (e: Exception) {
                        Logger.e("AuthViewModel", "Firebase user deletion failed.", e)
                        firebaseAuth.signOut()
                        _uiState.update { it.copy(requiresReAuthentication = true) }
                        return@launch
                    }
                }

                // 2. Credential Manager 상태 정리
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Logger.w("AuthViewModel", "Credential Manager clear failed.", e)
                }

                // 3. 카카오 계정 연결 해제
                try {
                    if (!_uiState.value.kakaoUid.isNullOrEmpty() && firebaseAuth.currentUser == null) {
                        unlinkKakaoAccount()
                    }
                } catch (e: Exception) {
                    Logger.w("AuthViewModel", "Kakao account unlink failed.", e)
                }

                // 4. WebView 완전 정리
                destroyWebViews()

                _uiState.value = AuthUiState(
                    isLoading = false,
                    isEnding = false,
                    userMessage = "계정이 성공적으로 삭제되었습니다."
                )
            } catch (e: Exception) {
                Logger.e("AuthViewModel", "Sign out process failed.", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isEnding = false,
                    userMessage = "계정 삭제 실패: ${e.message}"
                )
            }
        }
    }

    private fun unlinkKakaoAccount() {
        UserApiClient.instance.unlink { error ->
            if (error != null) {
                Logger.e("AuthViewModel", "Failed to unlink Kakao account", error)
            } else {
                Logger.d("AuthViewModel", "Successfully unlinked Kakao account")
                viewModelScope.launch { clearKakaoIdToken() }
            }
        }
    }
}