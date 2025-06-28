package com.peachspot.legendkofarm.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.util.Log
import android.webkit.WebView
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import com.peachspot.legendkofarm.data.remote.api.MyApiService
import com.peachspot.legendkofarm.data.repositiory.HomeRepository
import com.peachspot.legendkofarm.data.repositiory.UserPreferencesRepository
import com.peachspot.legendkofarm.data.repositiory.UserProfileData
import com.peachspot.legendkofarm.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.peachspot.legendkofarm.R
import com.peachspot.legendkofarm.data.remote.client.NetworkClient
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

data class HomeUiState(
    val errorMessage: String? = null,
    val userMessage: String? = null, // 사용자에게 표시할 메시지 (Snackbar 등)
    val userMessageType: String? = "info", //
    val weight: String = "",
    val isUserLoggedIn: Boolean = false,
    val userName: String? = null,
    val userEmail: String? = null,
    val userPhotoUrl: String? = null,
    val isLoading: Boolean = false,
    val firebaseUid: String? = null,
    val signInPendingIntent: IntentSender? = null, // 로그인 UI 시작을 위한 IntentSender


    var idToken: String? = null, // Firebase ID 토큰 저장

    val requiresReAuthentication: Boolean = false, // 재인증 필요 여부
    val termsAccepted: Boolean = false,
    val totalDistance: Double = 0.0, // 예시: 누적 거리를 Double로 가정
    val isLoadingTotalDistance: Boolean = true // 누적 거리 로딩 상태
)


class HomeViewModel (
    private val application: Application,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val firebaseAuth: FirebaseAuth,
    private val credentialManager: CredentialManager,
    private val myApiService: MyApiService,
    private val homeRepository: HomeRepository,



    ): ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val webViewMap = mutableMapOf<String, WebView>()

    fun refreshWebView(tag: String) {

        val url = when(tag.lowercase()) {
            "news" -> "https://urdesk.co.kr/smartkofarmnews/"
            "home" ->"https://urdesk.co.kr/smartkofarm/"
            "diary" -> "https://urdesk.co.kr/smartkofarmdiary/"
            "exchange" -> "https://urdesk.co.kr/smartkofarmexchange/"
            else -> "refresh"           // 기본 URL (필요하면)
        }

            if(url == "refresh") {
                webViewMap[tag]?.reload()
            }else{
                webViewMap[tag]?.loadUrl(url)
            }

    }

    fun getOrCreateWebView(context: Context, tag: String, url: String): WebView {
        return webViewMap.getOrPut(tag) {
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
    }


    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }


    private val googleIdOption: GetGoogleIdOption by lazy {
        GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false) // 모든 계정 표시 (이전에 로그인했는지 여부와 관계없이)
            .setServerClientId(application.getString(R.string.default_web_client_id))
            .setAutoSelectEnabled(false) // 자동 선택 비활성화 (계정 선택기 항상 표시 목적)
            .build()
    }



    init {
        Logger.d("ProfileViewModel", "ViewModel 초기화 시작.")
        checkCurrentUser()

        Logger.d("ProfileViewModel", "ViewModel 초기화 완료.")


        viewModelScope.launch { // Launch a coroutine here
            userPreferencesRepository.agreeFlow.collect { agreeStatus ->
                _uiState.update { currentState ->
                    currentState.copy(termsAccepted = agreeStatus ?: false)
                }
            }
        }
    }


    private fun checkCurrentUser() {
        Logger.d("ProfileViewModel", "현재 사용자 확인 중")
        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser != null) {
            viewModelScope.launch {
                try {
                    val tokenResult = firebaseUser.getIdToken(true).await()
                    val idToken = tokenResult.token
                    _uiState.update {
                        it.copy(
                            isUserLoggedIn = true,
                            userName = firebaseUser.displayName,
                            userEmail = firebaseUser.email,
                            userPhotoUrl = firebaseUser.photoUrl?.toString(),
                            idToken = idToken,
                            firebaseUid = firebaseUser.uid,
                            isLoading = false
                        )
                    }
                    Logger.d("ProfileViewModel", "현재 사용자 확인됨, ID 토큰 갱신: $idToken")
                } catch (e: Exception) {
                    //Logger.e("ProfileViewModel", "ID 토큰 갱신 실패", e)
                    _uiState.update {
                        it.copy(
                            isUserLoggedIn = true, // Firebase에는 로그인 되어 있지만 토큰 가져오기 실패
                            userName = firebaseUser.displayName,
                            userEmail = firebaseUser.email,
                            userPhotoUrl = firebaseUser.photoUrl?.toString(),
                            idToken = null, // 토큰 가져오기 실패 시 null
                            isLoading = false,
                            userMessage = "세션 정보를 가져오는 데 실패했습니다.",
                            userMessageType = "error"
                        )
                    }
                }
            }
        } else {
            _uiState.update {
                it.copy(
                    isUserLoggedIn = false,
                    userName = null,
                    userEmail = null,
                    userPhotoUrl = null,
                    idToken = null,
                    isLoading = false
                )
            }
            Logger.d("ProfileViewModel", "현재 로그인된 사용자 없음.")
        }
    }

    fun startGoogleSignIn() {
        Logger.d("ProfileViewModel", "startGoogleSignIn 호출됨")
        _uiState.update {
            it.copy(
                isLoading = true,
                userMessage = null,
                signInPendingIntent = null // 이전 인텐트 초기화
            )
        }
        viewModelScope.launch {
            try {
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                Logger.d("ProfileViewModel", "CredentialManager에 credential 요청 중")
                // 이 호출은 credential을 바로 반환하거나, 사용자 상호작용이 필요하면 예외를 발생시킬 수 있습니다.
                val result: GetCredentialResponse =
                    credentialManager.getCredential(request = request, context = application)
                Logger.d("ProfileViewModel", "Credential을 직접 수신함.")
                handleSignInCredential(result.credential)
            } catch (e: GetCredentialException) {
                Logger.e(
                    "ProfileViewModel",
                    "startGoogleSignIn - GetCredentialException: ${e.javaClass.simpleName}",
                    e
                )
                //handleGetCredentialException(e, "Google 로그인 시작 실패")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userMessage = "Google 로그인 실패,플레이스토어에서 로그인을 하고 시작하세요",
                        userMessageType = "error",
                        signInPendingIntent = null
                    )
                }
            } catch (e: Exception) {
                Logger.e("ProfileViewModel", "startGoogleSignIn - 일반 예외", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userMessage = "로그인 중 알 수 없는 오류 발생: ${e.localizedMessage}",
                        signInPendingIntent = null
                    )
                }
            }
        }
    }


    // ProfileScreen에서 IntentSender (PendingIntent) 실행 후 결과를 받아 호출되는 함수
    fun handleSignInActivityResult(data: Intent?) {
        Logger.d("ProfileViewModel", "handleSignInActivityResult 호출됨, data: $data")
        _uiState.update { it.copy(isLoading = true, userMessage = null) }

        viewModelScope.launch {
            try {
                val extras = data?.extras
                    ?: throw IllegalStateException("로그인 결과 Intent 또는 Bundle이 null입니다.")

                val credential = GoogleIdTokenCredential.createFrom(extras)
                Logger.d("ProfileViewModel", "Credential 획득 성공: ${credential.idToken?.take(10)}...")

                handleSignInCredential(credential)

            } catch (e: GoogleIdTokenParsingException) {
                Logger.e("ProfileViewModel", "Google ID 토큰 파싱 실패", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userMessage = "Google ID 토큰 파싱 실패: ${e.localizedMessage}"
                    )
                }

            } catch (e: IllegalStateException) {
                Logger.e("ProfileViewModel", "데이터 상태 예외 발생", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userMessage = e.message ?: "로그인 처리 중 예기치 않은 오류가 발생했습니다."
                    )
                }

            } catch (e: Exception) {
                Logger.e("ProfileViewModel", "알 수 없는 예외 발생", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userMessage = "로그인 결과 처리 중 오류: ${e.localizedMessage ?: "알 수 없음"}"
                    )
                }
            }
        }
    }


    fun handleSignInCredential(credential: androidx.credentials.Credential) {
        Logger.d("ProfileViewModel", "handleSignInCredential 호출됨, type: ${credential.type}")
        if (!uiState.value.isLoading) {
            _uiState.update { it.copy(isLoading = true, userMessage = null) }
        }
        viewModelScope.launch {
            try {
                // credential.type을 직접 비교하고, 안전한 캐스팅을 시도합니다.
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    // GoogleIdTokenCredential로 캐스팅합니다.
                    // createFrom() 또는 CredentialManager에서 반환된 credential 객체는
                    // 이 타입일 것으로 예상됩니다.
                    val googleIdTokenCredential =
                        GoogleIdTokenCredential.createFrom(credential.data)

                    val googleIdToken = googleIdTokenCredential.idToken
                    Logger.d(
                        "ProfileViewModel",
                        "Google ID 토큰 획득 (type check and cast): $googleIdToken"
                    )
                    firebaseAuthWithGoogleToken(googleIdToken)
                } else {
                    val errorMsg = "지원되지 않는 인증 유형입니다: ${credential.type}"
                    _uiState.update {
                        it.copy(isLoading = false, userMessage = errorMsg)
                    }
                    Logger.w("ProfileViewModel", errorMsg)
                }
            } catch (e: GoogleIdTokenParsingException) {
                Logger.e(
                    "ProfileViewModel",
                    "handleSignInCredential - GoogleIdTokenParsingException",
                    e
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userMessage = "Google ID 토큰 파싱 실패: ${e.localizedMessage}"
                    )
                }
            } catch (e: IllegalArgumentException) { // createFrom() 에서 발생 가능
                Logger.e(
                    "ProfileViewModel",
                    "handleSignInCredential - IllegalArgumentException (createFrom)",
                    e
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userMessage = "인증 데이터 처리 실패: ${e.localizedMessage}"
                    )
                }
            } catch (e: Exception) {
                Logger.e("ProfileViewModel", "handleSignInCredential - 일반 예외", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userMessage = "로그인 처리 중 알 수 없는 오류: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    // UI에서 signInPendingIntent를 실행한 후 호출
    fun onSignInLaunched() {
        Logger.d("ProfileViewModel", "onSignInLaunched 호출됨, signInPendingIntent 초기화.")
        _uiState.update { it.copy(signInPendingIntent = null) }
    }

    // 인텐트 실행 실패 또는 다른 이유로 signInPendingIntent를 초기화할 때 호출
    fun clearSignInPendingIntent() {
        Logger.d("ProfileViewModel", "clearSignInPendingIntent 호출됨.")
        _uiState.update { it.copy(signInPendingIntent = null, isLoading = false) }
    }

    private fun firebaseAuthWithGoogleToken(idToken: String) {
        val authCredential = GoogleAuthProvider.getCredential(idToken, null)
        viewModelScope.launch {
            try {
                val authResult = firebaseAuth.signInWithCredential(authCredential).await()
                val firebaseUser = authResult.user
                if (firebaseUser != null) {
                    val tokenResult = firebaseUser.getIdToken(true).await() // ID 토큰 강제 갱신
                    val firebaseIdToken = tokenResult.token
                    val firebaseUid = firebaseUser.uid

                    val userProfileToSave = UserProfileData(
//                        googleId = null, // Google ID는 현재 가져오지 않으므로 null 또는 다른 적절한 값
//                        name = userName,
//                        email = userEmail,
//                        photoUrl = userPhotoUrl,
                        firebaseUid = firebaseUid // Firebase UID 저장
                    )
                    userPreferencesRepository.saveUserProfileData(userProfileToSave) // 생성한 객체를 전달

                    val fcmToken = FirebaseMessaging.getInstance().token.await()


                    if (!fcmToken.isNullOrBlank()) {
                        NetworkClient.myApiService.registerUser("AppToken", firebaseUid, fcmToken)
                    }else{
                        Logger.e("ProfileViewModel", "FCM 토큰이 null 또는 빈 문자열입니다.")
                    }


                    _uiState.update {
                        it.copy(
                            isUserLoggedIn = true,
                            userName = firebaseUser.displayName,
                            userEmail = firebaseUser.email,
                            userPhotoUrl = firebaseUser.photoUrl?.toString(),
                            isLoading = false,
                            firebaseUid = firebaseUid,
                            userMessage = "로그인 되었습니다."
                        )
                    }



                } else {
                    throw IllegalStateException("Firebase User is null after successful sign in.")
                }
            } catch (e: Exception) {
                val errorMsg =
                    "Firebase 인증 실패: ${e.localizedMessage ?: "알 수 없는 오류"}"
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userMessage = errorMsg,
                        isUserLoggedIn = false,
                        idToken = null
                    )
                }
                Logger.e("ProfileViewModel", "Firebase Google 인증 실패", e)
            }
        }
    }


    private fun handleGetCredentialException(e: GetCredentialException, contextMessage: String) {
        Logger.d("login error", contextMessage)
    }


    suspend fun sendJsonToServer(tableName: String, jsonData: String) {
        // 이미 suspend 함수이므로 viewModelScope.launch 불필요
        val storedProfileData: UserProfileData? =
            userPreferencesRepository.userProfileDataFlow.firstOrNull()
        val storedFirebaseUid = storedProfileData?.firebaseUid

        if (storedFirebaseUid == null) {
            Logger.e("ProfileViewModel", "Firebase UID is null. Cannot send JSON to server.")
            _uiState.update {
                it.copy(
                    userMessageType = "error",
                    userMessage = "사용자 인증 정보를 찾을 수 없습니다. 다시 로그인해주세요."
                )
            }
            return
        }

        try {
            // 수정된 MediaType 생성
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonData.toRequestBody(mediaType)

            val response =
                myApiService.uploadDatabaseDumpJson(tableName, storedFirebaseUid, requestBody)
            Logger.d("NICAP", "$tableName:SEND")

            if (response.isSuccessful) {
                Logger.d("ProfileViewModel", "JSON data sent to server successfully.")
                _uiState.update {
                    it.copy(
                        userMessageType = "success", // 성공 타입 명시
                        userMessage = "데이터가 서버에 성공적으로 전송되었습니다."
                    )
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Logger.e(
                    "ProfileViewModel",
                    "Failed to send JSON to server. Code: ${response.code()}, Error: $errorBody"
                )
                _uiState.update {
                    it.copy(
                        userMessageType = "error",
                        userMessage = "서버 전송 실패: ${response.message()} (코드: ${response.code()})"
                    )
                }
            }
        } catch (e: Exception) {
            Logger.e("ProfileViewModel", "Exception while sending JSON to server.", e)
            _uiState.update {
                it.copy(
                    userMessageType = "error",
                    userMessage = "서버 전송 중 오류 발생: ${e.localizedMessage}"
                )
            }
        }
    }

    fun signOut() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                firebaseAuth.signOut()
                Logger.d("ProfileViewModel", "Firebase에서 사용자 로그아웃됨.")
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                    Logger.d(
                        "ProfileViewModel",
                        "CredentialManager를 통해 Credential 상태가 성공적으로 지워졌습니다."
                    )
                } catch (e: ClearCredentialException) {
                    Logger.e(
                        "ProfileViewModel",
                        "CredentialManager를 통해 Credential 상태를 지우는 데 실패했습니다.",
                        e
                    )
                    // 치명적이지 않은 오류이므로 UI 업데이트 계속
                }

                _uiState.update { currentState ->
                    HomeUiState( // 상태를 초기화하되, 로컬 설정(몸무게 등)은 유지할 수 있습니다.
                        weight = currentState.weight, // 기존 몸무게 유지
                        isUserLoggedIn = false,
                        userName = null,
                        userEmail = null,
                        userPhotoUrl = null,
                        isLoading = false, // 로그아웃 완료
                        signInPendingIntent = null,
                        userMessage = null,
                        termsAccepted = true
                    )
                }
                Logger.d("ProfileViewModel", "로그아웃 절차 완료. UI 상태 초기화됨.")

            } catch (e: Exception) {
                Logger.e("ProfileViewModel", "로그아웃 중 오류 발생", e)
                _uiState.update {
                    it.copy(isLoading = false, userMessage = "로그아웃 중 오류 발생: ${e.localizedMessage}")
                }
            }
        }
    }

    fun toggleTermsAccepted() {
        val newTermsAccepted = !(_uiState.value.termsAccepted)
        Logger.d("NICAP", newTermsAccepted.toString())
        _uiState.update {
            it.copy(termsAccepted = newTermsAccepted)
        }
        viewModelScope.launch {
            userPreferencesRepository.saveAgree(newTermsAccepted)
        }
    }

    fun deleteUserAccount() {
        _uiState.update {
            it.copy(
                isLoading = true,
                userMessage = null,
                requiresReAuthentication = false
            )
        }

        viewModelScope.launch {
            val currentUser = firebaseAuth.currentUser
            val storedProfileData: UserProfileData? =
                userPreferencesRepository.userProfileDataFlow.firstOrNull()
            val storedFirebaseUid = storedProfileData?.firebaseUid
            if (storedFirebaseUid == null) {
                Logger.w("ProfileViewModel", "계정 삭제 시도: 생성된 계정 없음 (Firebase 비로그인, 로컬 UID 없음).")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userMessage = "생성된 계정이 없습니다. 삭제할 계정이 없습니다.",
                        isUserLoggedIn = false
                    )
                }
                return@launch
            }

            if (currentUser == null) {
                if (storedFirebaseUid != null && storedFirebaseUid.isNotBlank()) {
                    Logger.w(
                        "ProfileViewModel",
                        "계정 삭제 시도: 사용자가 Firebase에 로그인되어 있지 않음 (로컬에 UID 기록은 존재)."
                    )
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userMessage = "계정을 삭제하려면 먼저 로그인해주세요.",
                            isUserLoggedIn = false // 명시적으로 로그아웃 상태임을 알림
                        )
                    }
                }
                return@launch
            }

            val firebaseUidToDelete = currentUser.uid
            Logger.d(
                "ProfileViewModel",
                "Firebase 계정 및 백엔드 데이터 삭제 시도 중... 사용자 UID: $firebaseUidToDelete"
            )
            // --- 1. 백엔드 API 호출 ---
            Logger.d("ProfileViewModel", "백엔드 회원 데이터 삭제 API 호출 시작... UID: $firebaseUidToDelete")
            try {
                val apiResponse =
                    myApiService.deleteMemberData(firebaseUidToDelete) // myApiService 사용
                Logger.d("ProfileViewModel", "CredentialManager 상태 클리어 성공 (계정 삭제 후).")
            } catch (e: ClearCredentialException) {
                Logger.w("ProfileViewModel", "CredentialManager 상태 클리어 실패 (계정 삭제 후).")
            }
            // currentUser가 null이 아니므로, Firebase에 로그인된 사용자가 존재.
            Logger.d("ProfileViewModel", "Firebase 계정 삭제 시도 중... 사용자 UID: ${currentUser.uid}")
            try {
                currentUser.delete().await()
                Logger.d("ProfileViewModel", "Firebase 사용자 계정 삭제 성공.")

                // Credential Manager 상태 클리어
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                    Logger.d("ProfileViewModel", "CredentialManager 상태 클리어 성공 (계정 삭제 후).")
                } catch (e: ClearCredentialException) {
                    Logger.w("ProfileViewModel", "CredentialManager 상태 클리어 실패 (계정 삭제 후).")
                }

                // 로컬 DataStore에서 모든 사용자 프로필 정보 삭제
                userPreferencesRepository.clearUserProfileData() // UID 포함 모든 정보 삭제
                Logger.d("ProfileViewModel", "로컬 사용자 프로필 데이터 삭제됨 (계정 삭제 후).")
                // 필요하다면 다른 사용자 관련 데이터(예: 몸무게)도 여기서 초기화/삭제
                userPreferencesRepository.clearUserWeight()
                userPreferencesRepository.clearFirebaseUid()


                _uiState.update { // 상태를 초기화
                    HomeUiState(
                        // 기존 ProfileUiState의 기본값을 사용하거나, 필요한 값만 유지
                        weight = it.weight, // 몸무게는 유지하거나 필요에 따라 초기화
                        isUserLoggedIn = false,
                        userName = null,
                        userEmail = null,
                        userPhotoUrl = null,
                        idToken = null,
                        isLoading = false,
                        signInPendingIntent = null,
                        userMessage = "계정이 성공적으로 삭제되었습니다.",
                        requiresReAuthentication = false,


                        )
                }
            } catch (e: Exception) {
                startGoogleSignIn()
                Logger.w("ProfileViewModel", "Firebase 사용자 계정 삭제 실패")
                if (e is FirebaseAuthRecentLoginRequiredException) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userMessage = "계정을 삭제하려면 보안을 위해 다시 로그인해야 합니다.",
                            requiresReAuthentication = true // UI에서 재로그인 유도
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userMessage = "계정 삭제에 실패했습니다: ${e.localizedMessage ?: "알 수 없는 오류"}"
                        )
                    }
                }


            }
        }

    }


    fun needAgree() {
        _uiState.update { it.copy(userMessageType = "error", userMessage = "개인정보 취급 방침에 동의하여 주세요") }
    }

    fun clearuserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }



}

