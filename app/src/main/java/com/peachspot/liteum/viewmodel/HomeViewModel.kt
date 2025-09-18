package com.peachspot.liteum.viewmodel

import android.app.Activity
import android.app.Application
// import android.content.Context // 웹뷰 사용 안 함
import android.content.IntentSender
import android.net.Uri
import android.util.Log
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

import com.peachspot.liteum.data.repositiory.UserPreferencesRepository
import com.peachspot.liteum.data.repositiory.UserProfileData

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
import com.peachspot.liteum.data.db.BookLogs
import com.peachspot.liteum.data.db.BookLogsDao
import com.peachspot.liteum.data.db.ReviewLogs
import com.peachspot.liteum.data.model.FeedItem
import com.peachspot.liteum.data.repositiory.BookRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted // `feedItems` 선언에 사용
import kotlinx.coroutines.flow.catch // `feedItems` 선언에 사용
import kotlinx.coroutines.flow.onStart // `feedItems` 선언에 사용
import kotlinx.coroutines.flow.stateIn // `feedItems` 선언에 사용
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

// DataStore 정의는 유지
private val Application.dataStore by preferencesDataStore("secure_prefs")

// AuthUiState에서 webViewAuthUrl 제거, isLoadingFeed 추가
data class AuthUiState(
    val isUserLoggedIn: Boolean = false, // 초기값 false 권장
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
    private val bookRepository: BookRepository
) : ViewModel() {
    private fun formatDateToString(date: Date?): String? {
        return date?.let {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)
        }
    }
    private val _loginResult = MutableStateFlow<Result<String>?>(null)
    val loginResult: StateFlow<Result<String>?> = _loginResult
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun saveBookAndReview(
        bookTitle: String,
        selectedImageFilePath: String?, // Uri?에서 String?으로 변경 (파일 경로)
        reviewText: String,
        rating: Float,
        author: String?,
        publisher: String?,
        publishDate: Date?, // UI에서 Date? 그대로 받음
        isbn: String?,
        startDate: Date?, // UI에서 Date? 그대로 받음
        endDate: Date?, // UI에서 Date? 그대로 받음
        memberId: String,
        shareReview: String // "Y" 또는 "N"
    ) {
        viewModelScope.launch {
            try {
                // 1. BookLogs 객체 생성
                // coverImageUri는 Uri를 String으로 변환하여 저장합니다.
                // 날짜 관련 필드들은 String? 형태로 변환하여 저장합니다.
                val bookLog = BookLogs(
                    // id는 autoGenerate이므로 0L 또는 기본값으로 둡니다.
                    bookTitle = bookTitle,
                    coverImageUri = selectedImageFilePath?.toString() ?: "", // Uri를 String으로, null이면 빈 문자열
                    startReadDate = formatDateToString(startDate),
                    endReadDate = formatDateToString(endDate),
                    author = author,
                    member_id = memberId, // BookLogs를 생성한 사용자 ID
                    bookGenre = null, // 현재 UI에서 입력받지 않으므로 null 또는 기본값
                    publishDate = formatDateToString(publishDate),
                    isbn = isbn,
                    rating = rating, // BookLogs에도 평점 저장 (선택 사항)
                    pageCount = null, // 현재 UI에서 입력받지 않으므로 null 또는 기본값
                    publisher = publisher,
                    createdAtMillis = System.currentTimeMillis() // 현재 시간으로 생성 시간 기록
                )

                // 2. BookLogs를 데이터베이스에 삽입하고 생성된 ID를 받음
                val newBookLogId = bookRepository.insertBookLog(bookLog)

                if (newBookLogId > 0) { // 성공적으로 BookLog가 삽입되었다면 (ID가 0보다 크면 성공으로 간주)
                    // 3. ReviewLogs 객체 생성
                    val reviewLog = ReviewLogs(
                        // id는 autoGenerate이므로 0L 또는 기본값으로 둡니다.
                        bookLogLocalId = newBookLogId, // 위에서 받은 BookLog의 ID를 외래 키로 사용
                        reviewText = reviewText,
                        share = shareReview,
                        createdAtMillis = System.currentTimeMillis(),
                        updatedAtMillis = System.currentTimeMillis(), // 생성 시에는 생성 시간과 동일하게 설정
                        memberId = memberId // 리뷰를 작성한 사용자 ID
                    )

                    // 4. ReviewLogs를 데이터베이스에 삽입
                    bookRepository.insertReviewLog(reviewLog)

                    Log.d("HomeViewModel", "책과 리뷰가 성공적으로 저장되었습니다. BookLog ID: $newBookLogId")
                    // TODO: 저장 성공 UI 이벤트 발생 (예: LiveData/StateFlow 상태 업데이트)
                } else {
                    Log.e("HomeViewModel", "BookLog 저장 실패. 반환된 ID: $newBookLogId")
                    // TODO: BookLog 저장 실패 UI 이벤트 발생
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "책과 리뷰 저장 중 오류 발생", e)
                // TODO: 일반적인 저장 오류 UI 이벤트 발생
            }
        }
    }

    // getFeedItemsFlow()를 사용하여 StateFlow로 변환 (기존 방식 유지)
    val feedItems: StateFlow<List<FeedItem>> = bookRepository.getAllBookFeedItemsFlow() // 메서드 이름 수정
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
     * 이 함수는 `bookRepository.getFeedItemsFlow()`가 새로운 데이터를 방출하도록
     * Repository 레벨에서 데이터 소스를 갱신하는 로직을 트리거해야 합니다.
     * ViewModel에서 직접 `feedItems` StateFlow의 값을 변경하는 것이 아니라,
     * 데이터 소스의 변경이 Flow를 통해 자연스럽게 반영되도록 합니다.
     *
     * 현재 `bookRepository`에 명시적인 refresh 함수가 없다면,
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
        // 예를 들어, BookRepository에 refreshFeeds() 같은 함수가 있고,
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
        //         // 예시: bookRepository.refreshFeedData() // 이 함수가 내부적으로 데이터 소스를 갱신
        //         // 그러면 feedItems Flow가 자동으로 새 데이터를 받게 됨.
        //         // 또는 bookRepository.getFeedItems()를 호출하고 그 결과를 어딘가에 써야하는데,
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
                // 예: bookRepository.triggerFeedRefresh()
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
                                    isLoading = false
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
            return
        }

        // ID 토큰이 유효한 경우, 코루틴을 시작하여 비동기 작업 처리
        idToken?.let { token ->
            viewModelScope.launch {
                try {
                    // 1. 카카오 사용자 정보 비동기적으로 가져오기 (코루틴 사용)
                    val user = suspendCancellableCoroutine<com.kakao.sdk.user.model.User> { continuation ->
                        UserApiClient.instance.me { kakaoUser, err ->
                            if (err != null) {
                                continuation.resumeWith(Result.failure(err))
                            } else if (kakaoUser != null) {
                                continuation.resume(kakaoUser)
                            } else {
                                continuation.resumeWith(Result.failure(IllegalStateException("User information not available.")))
                            }
                        }
                    }

                    // 2. Firebase Cloud Messaging(FCM) 토큰 비동기적으로 가져오기
                    val fcmToken = FirebaseMessaging.getInstance().token.await()

                    if (!fcmToken.isNullOrBlank()) {
                        // 3. 사용자 등록 API 호출 (네트워크 에러 처리 추가)
                        val kakaoUid = user.id?.toString()
                        try {
                            // TODO: API 호출의 응답을 확인하고 적절히 처리해야 함
                            NetworkClient.myApiService.registerUser("", kakaoUid, fcmToken)
                        } catch (e: Exception) {
                            // API 호출 실패 시 에러 처리
                            Log.e("KakaoLogin", "API call failed", e)
                            _uiState.update { it.copy(userMessage = "사용자 등록에 실패했습니다.") }
                        }
                    }

                    // 4. UI 상태 업데이트
                    _loginResult.value = Result.success(token)
                    _uiState.update {
                        it.copy(
                            isUserLoggedIn = true,
                            kakaoUid = user.id?.toString(),
                            userName = user.kakaoAccount?.profile?.nickname,
                            userEmail = user.kakaoAccount?.email,
                            userPhotoUrl = user.kakaoAccount?.profile?.thumbnailImageUrl,
                            isLoading = false,
                            userMessage = null // 성공했으므로 메시지 초기화
                        )
                    }

                    // 5. ID 토큰 저장
                    saveKakaoIdToken(token)

                } catch (e: Exception) {
                    // 상위 레벨에서 발생하는 모든 예외 처리
                    Log.e("KakaoLogin", "Login process failed", e)
                    _loginResult.value = Result.failure(e)
                    _uiState.update { it.copy(isLoading = false, userMessage = e.localizedMessage) }
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
                NetworkClient.myApiService.registerUser(uid, "",fcmToken)
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

    fun getBookLogById(id: Long): Flow<BookLogs?> {
        return bookRepository.getBookLogById(id)
    }

    /**
     * BookLog를 업데이트하는 메서드
     */
    fun updateBookLog(bookLog: BookLogs) {
        viewModelScope.launch {
            try {
                val result = bookRepository.updateBookLog(bookLog)
                if (result > 0) {
                    // 업데이트 성공
                    Log.d("HomeViewModel", "BookLog updated successfully")
                } else {
                    // 업데이트 실패
                    Log.w("HomeViewModel", "BookLog update failed - no matching ID")
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error updating BookLog", e)
                // 에러 처리
            }
        }
    }
}
