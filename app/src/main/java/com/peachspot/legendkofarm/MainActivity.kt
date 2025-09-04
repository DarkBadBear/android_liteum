package com.peachspot.legendkofarm


import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.webkit.ValueCallback
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.google.firebase.remoteconfig.remoteConfig
import com.peachspot.legendkofarm.data.db.AppDatabase
import com.peachspot.legendkofarm.data.remote.client.NetworkClient.myApiService
import com.peachspot.legendkofarm.data.repository.HomeRepositoryImpl
import com.peachspot.legendkofarm.data.repository.UserPreferencesRepository
import com.kakao.sdk.common.KakaoSdk
import com.peachspot.legendkofarm.ui.profile.UserProfileViewModel

import com.peachspot.legendkofarm.ui.theme.legendkofarmTheme
import com.peachspot.legendkofarm.ui.url.UrlViewModel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import androidx.activity.viewModels
import com.peachspot.legendkofarm.dialogs.ShowForegroundNotificationDialog
import com.peachspot.legendkofarm.dialogs.ShowServiceStoppedDialogComposable
import com.peachspot.legendkofarm.dialogs.ShowUpdateDialogComposable
import com.peachspot.legendkofarm.ui.auth.AuthViewModel
import com.peachspot.legendkofarm.util.Logger
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var backPressedTime: Long = 0
    private val exitToastDuration: Int = 2000 // 2초 (밀리초 단위)
    private val TAG = "legendkofarm"
    private var showUpdateDialogState by mutableStateOf(false)
    private var updateInfoState by mutableStateOf<UpdateInfo?>(null)
    private var showServiceStoppedDialogState by mutableStateOf(false)
    private var showForegroundNotificationDialogState by mutableStateOf(false)
    private var foregroundNotificationDataState by mutableStateOf<Map<String, String>?>(null)


    private val urlViewModel: UrlViewModel by viewModels()



//
//    var filePathCallback: ValueCallback<Array<Uri>>? = null
//    var imageUri: Uri? = null


    data class UpdateInfo(
        val isForceUpdate: Boolean,
        val message: String,
        val storeUrl: String
    )

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Logger.d(TAG, "POST_NOTIFICATIONS permission granted.")
            } else {
                Logger.d(TAG, "POST_NOTIFICATIONS permission denied.")
                Toast.makeText(this, "알림 권한이 거부되었습니다. 일부 기능이 제한될 수 있습니다.", Toast.LENGTH_LONG).show()
            }
        }


    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val status = intent?.getStringExtra("status")
            val urlId = intent?.getLongExtra("urlId", -1) ?: -1
            if (status == "error" && urlId != -1L) {
                urlViewModel.loadUrlsFromRemote()
            }
        }
    }

    private val foregroundMessageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == MyFirebaseMessagingService.ACTION_FOREGROUND_MESSAGE) {
                val title = intent.getStringExtra(MyFirebaseMessagingService.EXTRA_TITLE)
                val body = intent.getStringExtra(MyFirebaseMessagingService.EXTRA_BODY)
                val link = intent.getStringExtra(MyFirebaseMessagingService.EXTRA_LINK)
                foregroundNotificationDataState = mapOf(
                    "title" to (title ?: "알림"),
                    "body" to (body ?: "새로운 메시지가 도착했습니다."),
                    "link" to (link ?: "")
                )
                showForegroundNotificationDialogState = true
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        FirebaseApp.initializeApp(this)
        Firebase.appCheck.installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )
        setupRemoteConfig()

        KakaoSdk.init(this, getString(R.string.kakao_app_key))


        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {

                val now = System.currentTimeMillis()
                if (now - backPressedTime < exitToastDuration) {
                        finish()
                } else {
                        Toast.makeText(
                            this@MainActivity,
                            "빠르게 한 번 더 누르면 종료됩니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                    backPressedTime = now
                }
            }
        })


        setContent {
            legendkofarmTheme {
                MainScreen() // ViewModel은 MainScreen 내부에서 생성

                LaunchedEffect(Unit) {
                    if (!isNetworkAvailable()) {
                        showServiceStoppedDialogState = true
                    } else {
                        checkAppVersion()
                        registerAppToken()
                    }
                }

                if (showServiceStoppedDialogState) {
                    ShowServiceStoppedDialogComposable {
                        finishAffinity()
                    }
                }

                updateInfoState?.let { info ->
                    ShowUpdateDialogComposable(
                        showDialog = showUpdateDialogState,
                        onDismissRequest = {
                            if (!info.isForceUpdate) showUpdateDialogState = false
                        },
                        isForceUpdate = info.isForceUpdate,
                        message = info.message,
                        storeUrl = info.storeUrl,
                        onUpdateClick = {
                            try {
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.storeUrl)))
                            } catch (e: ActivityNotFoundException) {
                                startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
                                )
                            }
                            finishAffinity()
                        }
                    )
                }

//                if (showForegroundNotificationDialogState) {
//                    foregroundNotificationDataState?.let { data ->
//                        ShowForegroundNotificationDialog(
//                            title = data["title"] ?: "알림",
//                            message = data["body"] ?: "새로운 메시지가 도착했습니다.",
//                            link = data["link"],
//                            onDismiss = { showForegroundNotificationDialogState = false },
//                            onConfirm = {
//                                showForegroundNotificationDialogState = false
//                                data["link"]?.takeIf { it.isNotBlank() }?.let { url ->
//                                    try {
//                                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
//                                    } catch (e: ActivityNotFoundException) {
//                                        Toast.makeText(this@MainActivity, "연결된 앱을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
//                                    }
//                                }
//                            }
//                        )
//                    }
//                }


            }
        }

        requestNotificationPermissionIfNeeded()
    }

    // closeApp 함수는 사용되지 않으므로 제거하거나 필요한 경우 유지
    fun closeApp(activity: Activity) {
        activity.finishAffinity()
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork =
                connectivityManager.getNetworkCapabilities(network) ?: return false

            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
                else -> false
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return networkInfo.isConnected
        }
    }

    override fun onStart() {
        super.onStart()
//        LocalBroadcastManager.getInstance(this).registerReceiver(
//            foregroundMessageReceiver,
//            IntentFilter(MyFirebaseMessagingService.ACTION_FOREGROUND_MESSAGE)
//        )
    }

    override fun onStop() {
        super.onStop()
     //   LocalBroadcastManager.getInstance(this).unregisterReceiver(foregroundMessageReceiver)
    }




    // requestLocationPermissionIfNeeded 함수는 사용되지 않으므로 제거하거나 필요한 경우 유지
    private fun requestLocationPermissionIfNeeded() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Logger.d(TAG, "POST_NOTIFICATIONS permission already granted.")
                }

                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    Logger.d(TAG, "Showing rationale for POST_NOTIFICATIONS permission.")
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }

                else -> {
                    Logger.d(TAG, "Requesting POST_NOTIFICATIONS permission.")
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            Logger.d(
                TAG,
                "Notification permission is not required to be requested at runtime for API < 33."
            )
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        const val ACTION_FOREGROUND_MESSAGE = "com.peachspot.legendkofarm.ACTION_FOREGROUND_MESSAGE"
        const val EXTRA_TITLE = "com.peachspot.legendkofarm.EXTRA_TITLE"
        const val EXTRA_BODY = "com.peachspot.legendkofarm.EXTRA_BODY"
        const val EXTRA_LINK = "com.peachspot.legendkofarm.EXTRA_LINK"
    }

    private fun setupRemoteConfig() {
        val remoteConfig = Firebase.remoteConfig
        val configSettings = com.google.firebase.remoteconfig.remoteConfigSettings {
            minimumFetchIntervalInSeconds =
                if (com.peachspot.legendkofarm.BuildConfig.DEBUG) {
                    0
                } else {
                    3600
                }
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Logger.d(TAG, "Remote Config defaults loaded.")
                } else {
                    Logger.e(TAG, "Failed to load Remote Config defaults.", task.exception)
                }
            }
    }

    private fun registerAppToken() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    send_token_with_uid(token, uid)
                } else {
                    Logger.e("MainActivity", "FCM 토큰 가져오기 실패", task.exception)
                }
            }
    }

    private fun send_token_with_uid(token: String?, uid: String) {
        if (token.isNullOrEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // val data = mapOf("token" to token, "uid" to uid) // 사용되지 않음
                val response = myApiService.registerUser( uid, token)
            } catch (e: Exception) {
                Logger.e("Main", "Exception while sending token to server.", e)
            }
        }
    }

    private fun checkAppVersion() {
        val remoteConfig = Firebase.remoteConfig
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val updated = task.result
                    val getServiceRunning = remoteConfig.getBoolean("running")
                    if (!getServiceRunning) {
                        showServiceStoppedDialogState = true
                    } else {
                        val latestVersionCode = remoteConfig.getLong("latest_version_code")
                        val isForceUpdate = remoteConfig.getBoolean("is_force_update")
                        val updateMessage = remoteConfig.getString("update_message")
                        val storeUrl = remoteConfig.getString("store_url")
                        val currentVersionCode = com.peachspot.legendkofarm.BuildConfig.VERSION_CODE
                        Logger.d(
                            TAG,
                            "Current Version Code: $currentVersionCode, Latest Version Code: $latestVersionCode"
                        )

                        if (latestVersionCode > currentVersionCode) {
                            updateInfoState = UpdateInfo(
                                isForceUpdate,
                                updateMessage,
                                storeUrl.ifEmpty { "https://play.google.com/store/apps/details?id=$packageName" })
                            showUpdateDialogState = true
                        } else {
                            Logger.d(TAG, "App is up to date.")
                        }
                    }
                } else {
                    Logger.e(TAG, "Failed to fetch remote config.", task.exception)
                }
            }
    }



    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.let {
            val link = it.getStringExtra(MyFirebaseMessagingService.EXTRA_LINK)
            if (link != null) {
                Logger.d("MainActivity", "Link from notification: $link")
            }
        }
    }




}