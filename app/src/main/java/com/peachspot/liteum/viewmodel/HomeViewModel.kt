package com.peachspot.liteum.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Context // Tink 복구 로직에서 SharedPreferences 접근 시 필요
import android.content.IntentSender
import android.net.Uri
import android.util.Base64 // 안드로이드 표준 Base64 사용
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.datastore.core.DataStore // DataStore 사용 시 필요
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences // DataStore 오류 처리 시 사용
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore // DataStore 위임
import androidx.lifecycle.AndroidViewModel // ViewModel 대신 AndroidViewModel 사용 (Application context 필요)
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AesGcmKeyManager // 키 템플릿 사용
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider // Firebase Google 인증
import com.google.firebase.messaging.FirebaseMessaging
import com.kakao.sdk.user.UserApiClient
import com.kakao.sdk.user.model.User // 카카오 User 모델
import com.peachspot.liteum.R
import com.peachspot.liteum.data.db.BookLogs
//import com.peachspot.liteum.data.db.BookLogsDao // ViewModel에서 직접 Dao 사용 안 함 (Repository 통해)
import com.peachspot.liteum.data.db.ReviewLogs
import com.peachspot.liteum.data.model.FeedItem
import com.peachspot.liteum.data.remote.api.MyApiService
import com.peachspot.liteum.data.remote.client.NetworkClient
import com.peachspot.liteum.data.repositiory.BookRepository
import com.peachspot.liteum.data.repositiory.UserPreferencesRepository
import com.peachspot.liteum.data.repositiory.UserProfileData
import com.peachspot.liteum.util.Logger // 기존 로거 사용 (또는 android.util.Log로 통일)
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import org.json.JSONObject // ID 토큰 파싱용
import java.io.IOException // 예외 처리용
import java.nio.charset.Charset // Charset 명시
import java.security.GeneralSecurityException // Tink 예외 처리용
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

// DataStore 인스턴스 (Application Context 확장으로 정의하는 것이 일반적)
// 파일 최상단 또는 별도의 DI 파일에 정의
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "secure_prefs_datastore")


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
    val isLoadingFeed: Boolean = false
)

class HomeViewModel(
    // Application 컨텍스트를 받기 위해 AndroidViewModel을 상속하거나 생성자로 직접 받음
    private val application: Application,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val firebaseAuth: FirebaseAuth,
    private val credentialManager: CredentialManager,
    private val myApiService: MyApiService, // 생성자에서 직접 받음 (NetworkClient.myApiService 대신)
    private val bookRepository: BookRepository
) : AndroidViewModel(application) { // AndroidViewModel 상속 시 application 자동 주입

    private fun formatDateToString(date: Date?): String? {
        return date?.let {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)
        }
    }

    private val _loginResult = MutableStateFlow<Result<String>?>(null)
    val loginResult: StateFlow<Result<String>?> = _loginResult.asStateFlow() // asStateFlow() 추가

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun saveBookAndReview(
        bookTitle: String,
        selectedImageFilePath: String?,
        reviewText: String,
        rating: Float,
        author: String?,
        publisher: String?,
        publishDate: Date?,
        isbn: String?,
        startDate: Date?,
        endDate: Date?,
        memberId: String,
        shareReview: String
    ) {
        viewModelScope.launch {

            val currentMemberId: String? = if (_uiState.value.isUserLoggedIn) {
                _uiState.value.firebaseUid ?: _uiState.value.kakaoUid // Firebase UID 우선, 없으면 Kakao UID
            } else {
                null
            }

            // 2. 사용자 ID가 없으면 저장 로직을 진행하지 않음
            if (currentMemberId.isNullOrBlank()) {
                Log.e("HomeViewModel", "User is not logged in. Cannot save book and review.")
                _uiState.update { it.copy(userMessage = "로그인이 필요합니다.", userMessageType = "error") }
                return@launch
            }


            try {
                val bookLog = BookLogs(
                    bookTitle = bookTitle,
                    coverImageUri = selectedImageFilePath ?: "",
                    startReadDate = formatDateToString(startDate),
                    endReadDate = formatDateToString(endDate),
                    author = author,
                    member_id = currentMemberId,
                    bookGenre = null,
                    publishDate = formatDateToString(publishDate),
                    isbn = isbn,
                    rating = rating,
                    pageCount = null,
                    publisher = publisher,
                    createdAtMillis = System.currentTimeMillis()
                )

                val newBookLogId = bookRepository.insertBookLog(bookLog)

                if (newBookLogId > 0) {
                    val reviewLog = ReviewLogs(
                        bookLogLocalId = newBookLogId,
                        reviewText = reviewText,
                        share = shareReview,
                        createdAtMillis = System.currentTimeMillis(),
                        updatedAtMillis = System.currentTimeMillis(),
                        memberId = currentMemberId
                    )
                    bookRepository.insertReviewLog(reviewLog)
                    Log.d("HomeViewModel", "Book and review saved successfully. BookLog ID: $newBookLogId")
                    _uiState.update { it.copy(userMessage = "독서 기록이 저장되었습니다.", userMessageType = "success") } // 성공 메시지
                } else {
                    Log.e("HomeViewModel", "BookLog save failed. Returned ID: $newBookLogId")
                    _uiState.update { it.copy(userMessage = "책 정보 저장 실패", userMessageType = "error") }
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error saving book and review", e)
                _uiState.update { it.copy(userMessage = "저장 중 오류 발생: ${e.message}", userMessageType = "error") }
            }
        }
    }

//    val feedItems: StateFlow<List<FeedItem>> = bookRepository.getAllBookFeedItemsFlow()
//        .onStart {
//            Log.d("HomeViewModel", "feedItems flow started, isLoadingFeed = true") // Logger 대신 Log 사용
//            _uiState.update { it.copy(isLoadingFeed = true, userMessage = null) }
//        }
//        .map { items ->
//            Log.d("HomeViewModel", "feedItems flow received ${items.size} items, isLoadingFeed = false")
//            _uiState.update { it.copy(isLoadingFeed = false, userMessage = null) }
//            items
//        }
//        .catch { e ->
//            Log.e("HomeViewModel", "Error loading feed items flow", e)
//            _uiState.update {
//                it.copy(
//                    isLoadingFeed = false,
//                    userMessage = "피드 로딩 오류: ${e.message ?: "알 수 없는 오류"}"
//                )
//            }
//            emit(emptyList())
//        }
//        .stateIn(
//            scope = viewModelScope,
//            started = SharingStarted.WhileSubscribed(5000),
//            initialValue = emptyList()
//        )

    init {
        viewModelScope.launch {
            checkCurrentUser()
        }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null, userMessageType = null) }
    }

    fun loadFeedItems(forceRefresh: Boolean = false) {
        if (!forceRefresh && _uiState.value.isLoadingFeed) {
            Log.d("HomeViewModel", "loadFeedItems called but already loading or not forcing refresh.")
            return
        }
        Log.d("HomeViewModel", "loadFeedItems called with forceRefresh: $forceRefresh. Triggering feed refresh.")
        if (forceRefresh) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoadingFeed = true) }
                // Repository에 새로고침 트리거 요청 (이런 함수가 Repository에 있다고 가정)
                // 예: bookRepository.refreshFeeds()
                // 이 호출 후, feedItems Flow가 새 데이터를 받으면 map 연산자에서 isLoadingFeed가 false로 바뀜.
                // 실제 구현은 Repository의 데이터 소스 업데이트 방식에 따라 달라짐.
                // 여기서는 UI 로딩 상태만 관리하고, 실제 데이터는 Flow에 의존한다고 가정.
                Log.d("HomeViewModel", "Force refresh requested. Assuming repository will update the flow or needs manual trigger here.")
                // 만약 Repository에 명시적 새로고침 함수가 없다면, 여기서 Repository의 데이터 가져오는 함수를 다시 호출해야 할 수도 있으나,
                // Flow 기반에서는 Repository 내부에서 데이터 소스를 갱신하는 것이 이상적입니다.
            }
        }
    }

    private val googleIdOption: GetGoogleIdOption by lazy {
        GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(application.getString(R.string.default_web_client_id))
            .setAutoSelectEnabled(false)
            .build()
    }

    // --- Tink 암호화 설정 ---
    private val KEYSET_PREF_FILE_NAME = "master_prefs_tink" // SharedPreferences 파일 이름
    private val KEYSET_NAME = "master_keyset_tink" // 키셋 이름
    private val MASTER_KEY_URI = "android-keystore://master_key_app" // Keystore 마스터 키 별칭 (고유하게)
    private val ASSOCIATED_DATA_FOR_TOKEN = application.packageName.toByteArray(Charsets.UTF_8) // 추가 인증 데이터

    // DataStore Preferences Key (카카오 ID 토큰 암호화용)
    private val encryptedKakaoTokenKey = stringPreferencesKey("encrypted_kakao_id_token_v1")

    private val aead: Aead by lazy {
        try {
            AeadConfig.register()
            Log.d("TinkInit", "Attempting to initialize AEAD...")
            initializeAeadInternal()
        } catch (e: Exception) {
            Log.e("TinkInit", "Initial AEAD initialization failed. Attempting recovery.", e)
            try {
                val sharedPreferences = application.getSharedPreferences(KEYSET_PREF_FILE_NAME, Context.MODE_PRIVATE)
                sharedPreferences.edit().remove(KEYSET_NAME).apply() // 문제의 키셋 삭제
                Log.w("TinkInit", "Cleared potentially problematic keyset from SharedPreferences. Retrying AEAD initialization.")
                initializeAeadInternal() // 재생성 시도
            } catch (retryException: Exception) {
                Log.e("TinkInit", "AEAD initialization failed even after recovery attempt.", retryException)
                // 이 경우 앱의 암호화 기능이 동작하지 않음을 의미. 앱을 계속 진행하기 어려울 수 있음.
                // 사용자에게 알리거나, 특정 기능을 비활성화하는 등의 조치가 필요.
                // 여기서는 예외를 던져서 앱이 비정상 종료되도록 하거나 (개발 중 디버깅 용이)
                // 또는 안정적인 기본값을 반환하는 더미 Aead를 사용할 수 있지만, 권장하지 않음.
                throw IllegalStateException("AEAD could not be initialized. App may not function correctly.", retryException)
            }
        }
    }

    private fun initializeAeadInternal(): Aead {
        val keysetHandle: KeysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(application, KEYSET_NAME, KEYSET_PREF_FILE_NAME)
            .withKeyTemplate(AesGcmKeyManager.aes256GcmTemplate()) // Tink에서 제공하는 표준 템플릿
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle
        Log.d("TinkInit", "KeysetHandle obtained successfully for $MASTER_KEY_URI.")
        val primitive = keysetHandle.getPrimitive(Aead::class.java)
        Log.d("TinkInit", "AEAD primitive obtained successfully.")
        return primitive
    }
    // --- Tink 암호화 설정 끝 ---

    fun checkCurrentUser() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) } // 로딩 시작
            var loggedIn = false
            var currentFirebaseUid: String? = null
            var currentKakaoUid: String? = null
            var currentUserName: String? = null
            var currentUserEmail: String? = null
            var currentUserPhotoUrl: String? = null

            // Firebase 로그인 상태 확인
            firebaseAuth.currentUser?.let { user ->
                currentFirebaseUid = user.uid
                currentUserName = user.displayName ?: parseEmailFromIdToken(user.getIdToken(false).await()?.token ?: "") // Fallback
                currentUserEmail = user.email
                currentUserPhotoUrl = user.photoUrl?.toString()
                loggedIn = true
                Log.d("AuthCheck", "Firebase user logged in: $currentFirebaseUid")
            }

            // 카카오 로그인 상태 확인 (Firebase 로그인 안 된 경우)
            if (!loggedIn) {
                val kakaoToken = loadKakaoIdToken() // 저장된 암호화된 토큰 로드 및 복호화
                if (!kakaoToken.isNullOrEmpty()) {
                    // 카카오 사용자 정보 가져오기 (토큰이 유효한 경우)
                    // 주의: 저장된 토큰이 만료되었을 수 있으므로, 실제 API 호출로 유효성 검사가 필요할 수 있음
                    try {
                        val user = suspendCancellableCoroutine<User?> { continuation ->
                            UserApiClient.instance.me { kakaoUser, error ->
                                if (error != null) {
                                    Log.w("AuthCheck", "Kakao me() API failed with stored token.", error)
                                    continuation.resume(null)
                                } else {
                                    continuation.resume(kakaoUser)
                                }
                            }
                        }
                        user?.let {
                            currentKakaoUid = it.id.toString()
                            currentUserName = it.kakaoAccount?.profile?.nickname ?: currentUserName // Firebase 이름 없을 시 사용
                            currentUserEmail = it.kakaoAccount?.email ?: currentUserEmail // Firebase 이메일 없을 시 사용
                            currentUserPhotoUrl = it.kakaoAccount?.profile?.thumbnailImageUrl ?: currentUserPhotoUrl
                            loggedIn = true
                            Log.d("AuthCheck", "Kakao user identified via stored token: $currentKakaoUid")
                        }
                    } catch (e: Exception) {
                        Log.e("AuthCheck", "Error fetching Kakao user info with stored token", e)
                        // 저장된 토큰으로 사용자 정보 가져오기 실패 시 토큰 삭제 고려
                        clearKakaoIdToken()
                    }
                }
            }

            // 최종 UI 상태 업데이트
            _uiState.update {
                it.copy(
                    isUserLoggedIn = loggedIn,
                    firebaseUid = currentFirebaseUid,
                    kakaoUid = currentKakaoUid,
                    userName = currentUserName,
                    userEmail = currentUserEmail,
                    userPhotoUrl = currentUserPhotoUrl,
                    isLoading = false // 로딩 완료
                )
            }
        }
    }


    fun startKakaoSignIn(activity: Activity) {
        if (UserApiClient.instance.isKakaoTalkLoginAvailable(activity)) {
            UserApiClient.instance.loginWithKakaoTalk(activity) { token, error ->
                handleKakaoLoginResult(token?.idToken, error, activity)
            }
        } else {
            UserApiClient.instance.loginWithKakaoAccount(activity) { token, error ->
                handleKakaoLoginResult(token?.idToken, error, activity)
            }
        }
    }

    private fun handleKakaoLoginResult(idToken: String?, error: Throwable?, activity: Activity) {
        if (error != null) {
            Log.e("KakaoLogin", "Kakao login failed", error)
            _loginResult.value = Result.failure(error)
            _uiState.update { it.copy(isLoading = false, userMessage = "카카오 로그인 실패: ${error.localizedMessage}", userMessageType = "error") }
            return
        }

        idToken?.let { token ->
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                try {
                    val user = suspendCancellableCoroutine<User> { continuation ->
                        UserApiClient.instance.me { kakaoUser, meError ->
                            if (meError != null) {
                                continuation.resumeWith(Result.failure(meError))
                            } else if (kakaoUser != null) {
                                continuation.resume(kakaoUser)
                            } else {
                                continuation.resumeWith(Result.failure(IllegalStateException("Kakao user info not available.")))
                            }
                        }
                    }

                    val fcmToken = try { FirebaseMessaging.getInstance().token.await() } catch (e: Exception) { Log.e("FCM", "Failed to get FCM token", e); "" }

                    if (fcmToken.isNotBlank()) {
                        try {
                            // MyApiService 사용 (생성자 주입)
                            val response = myApiService.registerUser("", user.id?.toString() ?: "", fcmToken)
                            // TODO: API 응답 처리 (response.isSuccessful 등)
                            Log.d("KakaoLogin", "User registration API call with FCM token. Response: ${response.code()}")
                        } catch (e: Exception) {
                            Log.e("KakaoLogin", "User registration API call failed", e)
                            // _uiState.update { it.copy(userMessage = "사용자 등록 중 오류 발생") } // 선택적 UI 피드백
                        }
                    }

                    saveKakaoIdToken(token) // ID 토큰 암호화 저장

                    _loginResult.value = Result.success(token)
                    _uiState.update {
                        it.copy(
                            isUserLoggedIn = true,
                            kakaoUid = user.id?.toString(),
                            userName = user.kakaoAccount?.profile?.nickname,
                            userEmail = user.kakaoAccount?.email,
                            userPhotoUrl = user.kakaoAccount?.profile?.thumbnailImageUrl,
                            isLoading = false,
                            userMessage = "카카오 로그인 성공"
                        )
                    }
                } catch (e: Exception) {
                    Log.e("KakaoLogin", "Kakao login process failed", e)
                    _loginResult.value = Result.failure(e)
                    _uiState.update { it.copy(isLoading = false, userMessage = "카카오 처리 중 오류: ${e.localizedMessage}", userMessageType = "error") }
                }
            }
        } ?: run {
            Log.e("KakaoLogin", "Kakao ID token is null")
            _loginResult.value = Result.failure(IllegalStateException("Kakao ID token is null."))
            _uiState.update { it.copy(isLoading = false, userMessage = "카카오 로그인 정보 없음", userMessageType = "error") }
        }
    }

    private suspend fun saveKakaoIdToken(token: String) {
        if (token.isEmpty()) {
            Log.w("HomeViewModel", "Attempted to save an empty Kakao ID token.")
            return
        }
        try {
            val plainText = token.toByteArray(Charsets.UTF_8)
            val encryptedTokenBytes = aead.encrypt(plainText, ASSOCIATED_DATA_FOR_TOKEN)
            val encryptedTokenString = Base64.encodeToString(encryptedTokenBytes, Base64.DEFAULT)

            // Application Context를 통해 DataStore 접근
            getApplication<Application>().dataStore.edit { preferences ->
                preferences[encryptedKakaoTokenKey] = encryptedTokenString
            }
            Log.d("HomeViewModel", "Kakao ID token encrypted and saved successfully.")
        } catch (e: GeneralSecurityException) {
            Log.e("HomeViewModel", "Failed to encrypt Kakao ID token: ${e.message}", e)
        } catch (e: IOException) {
            Log.e("HomeViewModel", "IOException while saving encrypted Kakao ID token: ${e.message}", e)
        } catch (e: Exception) {
            Log.e("HomeViewModel", "An unexpected error occurred while saving Kakao ID token: ${e.message}", e)
        }
    }

    private suspend fun loadKakaoIdToken(): String? {
        return try {
            val preferences = getApplication<Application>().dataStore.data
                .catch { exception ->
                    if (exception is IOException) {
                        Log.e("HomeViewModel", "IOException while reading DataStore for Kakao ID token.", exception)
                        emit(emptyPreferences())
                    } else {
                        throw exception
                    }
                }
                .map { prefs ->
                    prefs[encryptedKakaoTokenKey]
                }
                .first()

            if (preferences.isNullOrEmpty()) {
                Log.d("HomeViewModel", "No encrypted Kakao ID token found in DataStore.")
                return null
            }

            val encryptedTokenBytes = Base64.decode(preferences, Base64.DEFAULT)
            val decryptedTokenBytes = aead.decrypt(encryptedTokenBytes, ASSOCIATED_DATA_FOR_TOKEN)
            val originalToken = String(decryptedTokenBytes, Charsets.UTF_8)

            Log.d("HomeViewModel", "Kakao ID token decrypted successfully.")
            originalToken
        } catch (e: GeneralSecurityException) {
            Log.e("HomeViewModel", "Failed to decrypt Kakao ID token (GeneralSecurityException): ${e.message}", e)
            clearKakaoIdToken()
            null
        } catch (e: IllegalArgumentException) {
            Log.e("HomeViewModel", "Failed to decode Kakao ID token (IllegalArgumentException): ${e.message}", e)
            clearKakaoIdToken()
            null
        } catch (e: IOException) {
            Log.e("HomeViewModel", "IOException while loading/decrypting Kakao ID token: ${e.message}", e)
            null
        } catch (e: Exception) {
            Log.e("HomeViewModel", "An unexpected error occurred while loading/decrypting Kakao ID token: ${e.message}", e)
            null
        }
    }

    private suspend fun clearKakaoIdToken() {
        try {
            getApplication<Application>().dataStore.edit { preferences ->
                preferences.remove(encryptedKakaoTokenKey)
            }
            Log.d("HomeViewModel", "Stored Kakao ID token cleared.")
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Failed to clear Kakao ID token: ${e.message}", e)
        }
    }


    fun startGoogleSignIn(activity: Activity) { // Activity context 추가 (CredentialManager 필요)
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, userMessage = null, signInPendingIntent = null) }
            try {
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                // CredentialManager.getCredential은 Activity Context가 필요할 수 있음
                val result: GetCredentialResponse = credentialManager.getCredential(
                    context = getApplication<Application>().applicationContext, // 또는 그냥 getApplication()
                    request = request
                )
                handleSignInCredential(result.credential)
            } catch (e: NoCredentialException) {
                Log.d("HomeViewModel", "Google Sign-In: No saved credentials. Need to show account picker.", e)
                _uiState.update { it.copy(isLoading = false, userMessage = "저장된 Google 계정이 없습니다.", userMessageType = "info") }
            } catch (e: GetCredentialException) {
                Log.e("HomeViewModel", "Google Sign-In GetCredentialException", e)
                _uiState.update { it.copy(isLoading = false, userMessage = "Google 로그인 실패: ${e.type} - ${e.message}", userMessageType = "error") }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Google Sign-In General Exception", e)
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
                Log.w("HomeViewModel", "Unsupported Google credential type: ${credential.type}")
                _uiState.update { it.copy(isLoading = false, userMessage = "지원되지 않는 Google 인증 유형입니다.", userMessageType = "error") }
            }
        } catch (e: GoogleIdTokenParsingException) {
            Log.e("HomeViewModel", "GoogleIdTokenParsingException", e)
            _uiState.update { it.copy(isLoading = false, userMessage = "Google 토큰 파싱 실패: ${e.message}", userMessageType = "error") }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "handleSignInCredential General Exception", e)
            _uiState.update { it.copy(isLoading = false, userMessage = "Google 인증 처리 중 오류: ${e.message}", userMessageType = "error") }
        }
    }

    private suspend fun firebaseAuthWithGoogleToken(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        try {
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val user = authResult.user ?: throw Exception("Firebase User is null after Google sign-in")

            val emailFromToken = parseEmailFromIdToken(idToken) // ID 토큰에서 직접 이메일 파싱

            val userProfile = UserProfileData(firebaseUid = user.uid, googleId = user.providerData.find { it.providerId == GoogleAuthProvider.PROVIDER_ID }?.uid, email = emailFromToken ?: user.email)
            userPreferencesRepository.saveUserProfileData(userProfile) // UserPreferencesRepository 사용

            _uiState.update {
                it.copy(
                    isUserLoggedIn = true,
                    isLoading = false,
                    firebaseUid = user.uid,
                    userName = user.displayName ?: emailFromToken, // 이름 우선순위
                    userEmail = emailFromToken ?: user.email, // 이메일 우선순위
                    userPhotoUrl = user.photoUrl?.toString(),
                    userMessage = "Google 로그인 성공"
                )
            }

            val fcmToken = try { FirebaseMessaging.getInstance().token.await() } catch (e: Exception) { Log.e("FCM", "Failed to get FCM token", e); "" }
            if (fcmToken.isNotBlank()) {
                try {
                    val response = myApiService.registerUser(user.uid, "", fcmToken)
                    Log.d("GoogleLogin", "User registration API call with FCM token. Response: ${response.code()}")
                } catch (e: Exception) {
                    Log.e("GoogleLogin", "User registration API call failed", e)
                }
            }
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Firebase Auth with Google failed", e) // 태그 일관성
            _uiState.update {
                it.copy(
                    isLoading = false,
                    userMessage = "Google 인증 실패: ${e.localizedMessage}",
                    isUserLoggedIn = false,
                    firebaseUid = null,
                    userEmail = null
                )
            }
        }
    }

    private fun parseEmailFromIdToken(idToken: String): String? {
        // ID 토큰이 null이거나 비어있는 경우를 먼저 처리
        if (idToken.isBlank()) {
            Log.w("TokenParse", "ID token is blank, cannot parse email.")
            return null
        }
        return try {
            val parts = idToken.split(".")
            if (parts.size == 3) { // JWT는 헤더, 페이로드, 서명의 세 부분으로 구성
                val payload = String(Base64.decode(parts[1], Base64.URL_SAFE), Charsets.UTF_8)
                val json = JSONObject(payload)
                json.optString("email", null) // "email" 필드가 없을 경우 null 반환
            } else {
                Log.w("TokenParse", "Invalid ID token format (parts count: ${parts.size})")
                null
            }
        } catch (e: Exception) {
            Log.e("TokenParse", "Failed to parse email from ID token", e)
            null
        }
    }


    fun logOut() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isEnding = true) } // isEnding 사용
            try {
                firebaseAuth.signOut()
                clearKakaoIdToken() // 카카오 토큰도 삭제
                UserApiClient.instance.logout { error -> // 카카오 SDK 로그아웃
                    if (error != null) {
                        Log.e("Logout", "Kakao SDK logout failed", error)
                    } else {
                        Log.d("Logout", "Kakao SDK logout successful")
                    }
                }
                // Credential Manager 상태 클리어 (선택적이지만 권장)
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                    Log.d("Logout", "CredentialManager state cleared.")
                } catch (e: Exception) {
                    Log.e("Logout", "Failed to clear CredentialManager state.", e)
                }

                _uiState.value = AuthUiState( // 상태 초기화
                    isUserLoggedIn = false,
                    isLoading = false,
                    isEnding = false, // isEnding 다시 false로
                    userMessage = "로그아웃 되었습니다."
                )
            } catch (e: Exception) {
                Log.e("HomeViewModel", "LogOut failed", e)
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


    fun signOut(activity: Activity) { // 재인증 시 Activity Context 필요 가능성
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isEnding = true) }
            val firebaseUser = firebaseAuth.currentUser
            val kakaoLoggedIn = !_uiState.value.kakaoUid.isNullOrEmpty() // kakaoUid로 카카오 로그인 여부 판단

            try {
                // Firebase 계정 삭제 시도
                firebaseUser?.delete()?.await()
                Log.d("HomeViewModel", "Firebase user account deleted successfully.")

                // 카카오 연결 해제
                if (kakaoLoggedIn) { // 카카오 UID가 있다면 연결 해제 시도
                    suspendCancellableCoroutine<Boolean> { continuation ->
                        UserApiClient.instance.unlink { error ->
                            if (error != null) {
                                Log.e("SignOut", "Failed to unlink Kakao account", error)
                                continuation.resumeWith(Result.failure(error))
                            } else {
                                Log.d("SignOut", "Successfully unlinked Kakao account")
                                continuation.resume(true)
                            }
                        }
                    }
                }
                clearKakaoIdToken() // 저장된 카카오 토큰 삭제

                // Credential Manager 상태 클리어
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                    Log.d("SignOut", "CredentialManager state cleared.")
                } catch (e: Exception) {
                    Log.e("SignOut", "Failed to clear CredentialManager state.", e)
                }

                _uiState.value = AuthUiState(
                    isUserLoggedIn = false,
                    isLoading = false,
                    isEnding = false,
                    userMessage = "계정이 성공적으로 삭제되었습니다."
                )
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Sign out process failed.", e)
                if (e is com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
                    // 재인증 필요 처리 - UI에서 재로그인 유도 후 다시 signOut 시도
                    // 이 경우, startGoogleSignIn 또는 startKakaoSignIn을 다시 호출할 수 있도록 PendingIntent나 콜백 설정 필요
                    Log.w("HomeViewModel", "Re-authentication required for account deletion.")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isEnding = false,
                            requiresReAuthentication = true, // 재인증 필요 상태 설정
                            userMessage = "계정 삭제를 위해 재인증이 필요합니다. 다시 로그인해주세요.",
                            userMessageType = "error"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isEnding = false,
                            userMessage = "계정 삭제 실패: ${e.message}",
                            userMessageType = "error"
                        )
                    }
                }
            }
        }
    }

    // unlinkKakaoAccount 함수는 signOut 내부 로직으로 통합 또는 별도 유지 가능
    // 여기서는 signOut 내부에 통합함.

    override fun onCleared() {
        super.onCleared()
        Log.d("HomeViewModel", "onCleared")
    }

    fun getBookLogById(id: Long): Flow<BookLogs?> {
        return bookRepository.getBookLogById(id)
    }

    fun updateBookLog(bookLog: BookLogs) {
        viewModelScope.launch {
            try {
                val result = bookRepository.updateBookLog(bookLog)
                if (result > 0) {
                    Log.d("HomeViewModel", "BookLog updated successfully. ID: ${bookLog.id}")
                    // TODO: UI 업데이트 (예: _uiState.update { it.copy(userMessage = "업데이트 완료") })
                } else {
                    Log.w("HomeViewModel", "BookLog update failed - no matching ID or no change. ID: ${bookLog.id}")
                    // _uiState.update { it.copy(userMessage = "업데이트 실패", userMessageType = "error") }
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error updating BookLog. ID: ${bookLog.id}", e)
                // _uiState.update { it.copy(userMessage = "업데이트 중 오류: ${e.message}", userMessageType = "error") }
            }
        }
    }
}
