package com.peachspot.liteum.viewmodel

import android.app.Activity
import android.app.Application
// import android.content.Context // 웹뷰 사용 안 함
import android.content.IntentSender
// import androidx.compose.animation.core.copy // 현재 파일에서 사용 안 함
// import android.webkit.WebView // 웹뷰 사용 안 함
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
import com.peachspot.liteum.R
import com.peachspot.liteum.data.remote.api.MyApiService
import com.peachspot.liteum.data.remote.client.NetworkClient
import com.peachspot.liteum.data.repositiory.HomeRepository
import com.peachspot.liteum.data.repositiory.UserPreferencesRepository
import com.peachspot.liteum.data.repositiory.UserProfileData
import com.peachspot.liteum.ui.screens.FeedItem // HomeScreen에서 정의한 FeedItem 임포트
import com.peachspot.liteum.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map // `feedItems` 선언에 사용
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
// import kotlinx.coroutines.delay // 웹뷰 관련 로직에서 사용되던 것
// 웹뷰 관련 임포트 제거
import androidx.credentials.exceptions.NoCredentialException
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.kakao.sdk.user.UserApiClient
import com.kakao.sdk.user.model.User // 카카오 User 모델 직접 사용을 위해
import kotlinx.coroutines.flow.SharingStarted // `feedItems` 선언에 사용
import kotlinx.coroutines.flow.catch // `feedItems` 선언에 사용
import kotlinx.coroutines.flow.onStart // `feedItems` 선언에 사용
import kotlinx.coroutines.flow.stateIn // `feedItems` 선언에 사용

// DataStore 정의는 유지
private val Application.dataStore by preferencesDataStore("secure_prefs")

// AuthUiState에서 webViewAuthUrl 제거, isLoadingFeed 추가
data class AuthUiState(
    val isUserLoggedIn: Boolean = true, // 초기값 false 권장
    val isLoading: Boolean = true, // 전체 앱 로딩 (사용자 정보 등)
    val isEnding: Boolean = false, // 앱 종료 과정
    val userMessage: String? = null,
    val userMessageType: String? = null,
    val signInPendingIntent: IntentSender? = null,
    val firebaseUid: String? = null,
    val kakaoUid: String? = null,
    val userName: String? = null,
    val userEmail: String? = null,
    val userPhotoUrl: String? = null,
    val requiresReAuthentication: Boolean = false,
    val isLoadingFeed: Boolean = false // 피드 로딩 상태
)

class HomeViewModel(
    private val application: Application,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val firebaseAuth: FirebaseAuth,
    private val credentialManager: CredentialManager,
    private val myApiService: MyApiService,
    private val homeRepository: HomeRepository,
) : ViewModel() {

    private val _loginResult = MutableStateFlow<Result<String>?>(null)
    val loginResult: StateFlow<Result<String>?> = _loginResult
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // getFeedItemsFlow()를 사용하여 StateFlow로 변환 (기존 방식 유지)
    val feedItems: StateFlow<List<FeedItem>> = homeRepository.getAllBookFeedItemsFlow() // 메서드 이름 수정
        .onStart {
            Logger.d("HomeViewModel", "feedItems flow started, isLoadingFeed = true")
            _uiState.update { it.copy(isLoadingFeed = true, userMessage = null) } // 에러 메시지 초기화
        }
        .map { items ->
            Logger.d("HomeViewModel", "feedItems flow received ${items.size} items, isLoadingFeed = false")
            _uiState.update { it.copy(isLoadingFeed = false, userMessage = null) } // 성공 시 에러 메시지 제거
            items
        }
        .catch { e ->
            Logger.e("HomeViewModel", "Error loading feed items flow", e)
            _uiState.update {
                it.copy(
                    isLoadingFeed = false,
                    userMessage = "피드 로딩 오류: ${e.message ?: "알 수 없는 오류"}"
                )
            }
            emit(emptyList()) // 에러 시 빈 리스트 방출
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            checkCurrentUser() // 사용자 정보 먼저 확인
            // loadFeedItems()는 명시적 새로고침 용도로 남겨두거나,
            // Repository의 Flow가 자동으로 데이터를 가져오므로 init에서 호출할 필요가 없을 수 있음.
            // 만약 초기 로딩을 보장하고 싶다면, feedItems Flow가 구독될 때 onStart에서 처리.
            // 여기서는 `loadFeedItems()` 호출을 제거하거나, 다른 의미로 사용해야 함.
            // 현재 `feedItems`는 구독 시 자동으로 데이터를 가져오므로 `loadFeedItems()`를 `init`에서 호출할 필요는 없음.
        }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null, userMessageType = null) }
    }

    /**
     * 피드를 새로고침합니다.
     * 이 함수는 `homeRepository.getFeedItemsFlow()`가 새로운 데이터를 방출하도록
     * Repository 레벨에서 데이터 소스를 갱신하는 로직을 트리거해야 합니다.
     * ViewModel에서 직접 `feedItems` StateFlow의 값을 변경하는 것이 아니라,
     * 데이터 소스의 변경이 Flow를 통해 자연스럽게 반영되도록 합니다.
     *
     * 현재 `homeRepository`에 명시적인 refresh 함수가 없다면,
     * 이 함수는 아래와 같이 Repository의 `getFeedItems()` (suspend 함수)를 호출하여
     * 데이터를 가져오고, 그 결과를 별도의 MutableStateFlow (만약 사용한다면)에 할당하거나,
     * 또는 이 함수 자체가 다른 역할을 해야 합니다.
     *
     * **현재 `feedItems`의 선언 방식에서는 이 함수가 직접 `_feedItems.value = items`와 같이
     * 업데이트 할 수 없습니다. (`_feedItems`라는 MutableStateFlow가 없음)**
     */
    fun loadFeedItems(forceRefresh: Boolean = false) {
        // `feedItems`가 Flow를 통해 데이터를 받고 있으므로, 이 함수는
        // Repository에 "새로고침"을 요청하는 형태로 구현되어야 합니다.
        // 예를 들어, HomeRepository에 refreshFeeds() 같은 함수가 있고,
        // 그 함수가 내부적으로 데이터 소스를 업데이트하여 getFeedItemsFlow()가 새 값을 방출하도록 합니다.

        // 또는, 이 함수를 유지하고 싶다면 ViewModel 내부에 별도의 MutableStateFlow를 두고
        // 이 함수가 해당 MutableStateFlow를 업데이트하도록 구조를 변경해야 합니다. (이전 답변 참고)

        // 현재 구조에서의 임시적인 처리 (isLoadingFeed 상태만 업데이트):
        if (!forceRefresh && _uiState.value.isLoadingFeed) {
            Logger.d("HomeViewModel", "loadFeedItems called but already loading or not forcing refresh.")
            return
        }
        Logger.d("HomeViewModel", "loadFeedItems called with forceRefresh: $forceRefresh. Triggering feed refresh (conceptually).")
        // viewModelScope.launch { // 이 블록은 Repository의 refresh 로직을 호출해야 함
        //     _uiState.update { it.copy(isLoadingFeed = true) }
        //     try {
        //         // 예시: homeRepository.refreshFeedData() // 이 함수가 내부적으로 데이터 소스를 갱신
        //         // 그러면 feedItems Flow가 자동으로 새 데이터를 받게 됨.
        //         // 또는 homeRepository.getFeedItems()를 호출하고 그 결과를 어딘가에 써야하는데,
        //         // 현재 feedItems는 Flow 기반이라 직접 할당할 수 없음.
        //         Logger.d("HomeViewModel", "Feed refresh triggered (simulated).")
        //     } catch (e: Exception) {
        //         Logger.e("HomeViewModel", "Failed to trigger feed refresh (simulated)", e)
        //         _uiState.update { it.copy(userMessage = "피드 새로고침 실패: ${e.message}", userMessageType = "error") }
        //     } finally {
        //         // isLoadingFeed는 feedItems Flow의 onStart/map에서 관리되므로 여기서 직접 false로 바꿀 필요 없을 수 있음.
        //         // _uiState.update { it.copy(isLoadingFeed = false) }
        //     }
        // }

        // 현재로서는 이 함수가 하는 실제 데이터 로딩 역할이 모호합니다.
        // `feedItems`가 Flow로 데이터를 받고 있기 때문입니다.
        // 새로고침 UI(예: SwipeRefreshLayout)와 연동하려면,
        // 이 함수는 Repository에 새로고침을 요청하고, 로딩 상태만 관리하는 것이 적절합니다.
        if (forceRefresh) {
            viewModelScope.launch {
                // UI에 로딩 상태를 먼저 반영
                _uiState.update { it.copy(isLoadingFeed = true) }
                // Repository에 데이터 새로고침을 요청 (이런 함수가 Repository에 있다고 가정)
                // 예: homeRepository.triggerFeedRefresh()
                // 이 호출 후, feedItems Flow가 새 데이터를 받으면 map 연산자에서 isLoadingFeed가 false로 바뀜.
                // 만약 triggerFeedRefresh가 오래 걸리고 즉각적인 피드백이 필요하면, 여기서 잠시 후 로딩을 끌 수 있으나
                // Flow의 데이터 수신 시점으로 제어하는 것이 더 정확함.
                Logger.d("HomeViewModel", "Force refresh requested. Assuming repository will update the flow.")

                // 임시: 강제 새로고침 시 로딩 상태를 잠시 보여주고,
                // 실제 데이터 업데이트는 Flow에 의존한다고 가정.
                // 만약 Repository에 명시적인 refresh trigger가 없다면,
                // 이 함수는 UI의 로딩 인디케이터를 보여주는 역할만 하고,
                // 실제 데이터 업데이트는 Flow의 자연스러운 흐름에 맡길 수 있습니다.
            }
        }
    }


    // ---------------------- Google 로그인 옵션 ----------------------
    private val googleIdOption: GetGoogleIdOption by lazy {
        GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false) // 로그인 시 항상 계정 선택 창 표시 (필요에 따라 true로 변경 가능)
            .setServerClientId(application.getString(R.string.default_web_client_id)) // Firebase Console의 웹 클라이언트 ID
            .setAutoSelectEnabled(false) // 자동 선택 비활성화 (명시적 사용자 선택)
            .build()
    }

    // ---------------------- Tink 암호화 초기화 (카카오용) ----------------------
    private val tokenKey = stringPreferencesKey("kakao_id_token")
    private val aead: Aead by lazy {
        AeadConfig.register()
        val keysetHandle: KeysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(application, "master_keyset", "master_prefs")
            .withKeyTemplate(com.google.crypto.tink.aead.AesGcmKeyManager.aes256GcmTemplate())
            .withMasterKeyUri("android-keystore://master_key") // Android Keystore 사용 권장
            .build()
            .keysetHandle
        keysetHandle.getPrimitive(Aead::class.java)
    }

    // ---------------------- 사용자 로그인 상태 체크 ----------------------
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
                                    //webViewAuthUrl = if (loggedIn) "https://peachspot.co.kr/lkfAuth" else null
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
                        //webViewAuthUrl = "https://peachspot.co.kr/lkfAuth"
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
                    //    webViewAuthUrl = "https://peachspot.co.kr/lkfAuth"
                    )
                }
            }
        }
    }

    private suspend fun saveKakaoIdToken(token: String) {
        val encrypted = aead.encrypt(token.toByteArray(), null)
        application.dataStore.edit { prefs ->
            prefs[tokenKey] = android.util.Base64.encodeToString(encrypted, android.util.Base64.DEFAULT)
        }
    }

    private suspend fun loadKakaoIdToken(): String? {
        return try {
            val base64 = application.dataStore.data.map { it[tokenKey] }.first()
            base64?.let {
                val decrypted = aead.decrypt(android.util.Base64.decode(it, android.util.Base64.DEFAULT), null)
                String(decrypted)
            }
        } catch (e: Exception) {
            Logger.e("HomeViewModel", "Failed to load/decrypt Kakao ID token", e)
            null // 복호화 실패 또는 로드 실패 시 null 반환
        }
    }

    private suspend fun clearKakaoIdToken() {
        application.dataStore.edit { it.remove(tokenKey) }
    }

    // ---------------------- Google 로그인 ----------------------
    fun startGoogleSignIn() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, userMessage = null, signInPendingIntent = null) }
            try {
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                val result: GetCredentialResponse = credentialManager.getCredential(request = request, context = application)
                handleSignInCredential(result.credential)
            } catch (e: NoCredentialException) {
                Logger.d("HomeViewModel", "Google Sign-In: No saved credentials. Need to show account picker.")
                _uiState.update { it.copy(isLoading = false, userMessage = "저장된 Google 계정이 없습니다.", userMessageType = "info") }
            } catch (e: GetCredentialException) {
                Logger.e("HomeViewModel", "Google Sign-In GetCredentialException", e)
                _uiState.update { it.copy(isLoading = false, userMessage = "Google 로그인 실패: ${e.message}", userMessageType = "error") }
            } catch (e: Exception) {
                Logger.e("HomeViewModel", "Google Sign-In General Exception", e)
                _uiState.update { it.copy(isLoading = false, userMessage = "Google 로그인 중 오류 발생: ${e.message}", userMessageType = "error") }
            }
        }
    }

    private suspend fun handleSignInCredential(credential: androidx.credentials.Credential) {
        try {
            if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                firebaseAuthWithGoogleToken(googleIdTokenCredential.idToken)
            } else {
                _uiState.update { it.copy(isLoading = false, userMessage = "지원되지 않는 Google 인증 유형입니다.", userMessageType = "error") }
            }
        } catch (e: GoogleIdTokenParsingException) {
            Logger.e("HomeViewModel", "GoogleIdTokenParsingException", e)
            _uiState.update { it.copy(isLoading = false, userMessage = "Google 토큰 파싱 실패: ${e.message}", userMessageType = "error") }
        } catch (e: Exception) {
            Logger.e("HomeViewModel", "handleSignInCredential General Exception", e)
            _uiState.update { it.copy(isLoading = false, userMessage = "Google 인증 처리 중 오류: ${e.message}", userMessageType = "error") }
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
              //  webViewAuthUrl = "https://peachspot.co.kr/lkfAuth"
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
                //webViewAuthUrl = null
            )
        }
    }
    private fun parseEmailFromIdToken(idToken: String): String? {
        return try {
            val parts = idToken.split(".")
            if (parts.size == 3) {
                val payload = String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE))
                val json = org.json.JSONObject(payload)
                json.optString("email", null)
            } else { null }
        } catch (e: Exception) {
            Logger.e("HomeViewModel", "Failed to parse email from ID token", e)
            null
        }
    }

    // ---------------------- 로그아웃 ----------------------
    fun logOut() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isEnding = true) }
            try {
                firebaseAuth.signOut()
                clearKakaoIdToken()
                // _feedItems.value = emptyList() // Flow 기반이므로 직접 할당 불가, Repository가 빈 리스트를 방출해야 함.
                // 로그아웃 시 feedItems Flow가 빈 리스트를 방출하도록 Repository 레벨에서 처리하거나,
                // UI 레벨에서 로그인 상태에 따라 feedItems를 표시할지 결정.
                // 여기서는 UI 상태 초기화만 수행.
                _uiState.value = AuthUiState(
                    isUserLoggedIn = false,
                    isLoading = false,
                    isEnding = false,
                    userMessage = "로그아웃 되었습니다."
                )
            } catch (e: Exception) {
                Logger.e("HomeViewModel", "LogOut failed", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isEnding = false,
                        userMessage = "로그아웃 실패: ${e.localizedMessage}",
                        userMessageType = "error"
                    )
                }
            }
        }
    }

    // ---------------------- 회원 탈퇴 ----------------------
    fun signOut() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isEnding = true) }
            val firebaseUser = firebaseAuth.currentUser
            val kakaoLoggedIn = !_uiState.value.kakaoUid.isNullOrEmpty()

            try {
                firebaseUser?.delete()?.await()
                Logger.d("HomeViewModel", "Firebase user account deleted successfully.")

                if (kakaoLoggedIn && firebaseUser == null) {
                    unlinkKakaoAccount()
                } else if (firebaseUser != null) {
                    clearKakaoIdToken()
                }
                // _feedItems.value = emptyList() // 위와 동일한 이유로 직접 할당 불가.
                _uiState.value = AuthUiState(
                    isUserLoggedIn = false,
                    isLoading = false,
                    isEnding = false,
                    userMessage = "계정이 성공적으로 삭제되었습니다."
                )
            } catch (e: Exception) {
                Logger.e("HomeViewModel", "Sign out process failed.", e)
                if (e is com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
                    _uiState.update {
                        it.copy(isLoading = false, isEnding = false, requiresReAuthentication = true, userMessage = "계정 삭제를 위해 재인증이 필요합니다.", userMessageType = "error")
                    }
                } else {
                    _uiState.update {
                        it.copy(isLoading = false, isEnding = false, userMessage = "계정 삭제 실패: ${e.message}", userMessageType = "error")
                    }
                }
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

    override fun onCleared() {
        super.onCleared()
        Logger.d("HomeViewModel", "onCleared")
    }
}
