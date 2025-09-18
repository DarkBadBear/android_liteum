package com.peachspot.liteum

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
import androidx.navigation.compose.rememberNavController
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.peachspot.liteum.data.db.AppDatabase
import com.peachspot.liteum.data.remote.client.NetworkClient.myApiService
import com.peachspot.liteum.data.repositiory.HomeRepositoryImpl
import com.peachspot.liteum.data.repositiory.UserPreferencesRepository
import com.peachspot.liteum.services.MyFirebaseMessagingService
import com.peachspot.liteum.ui.screens.MainScreen

import com.peachspot.liteum.ui.theme.liteumiTheme
import com.peachspot.liteum.viewmodel.HomeViewModel
import com.peachspot.liteum.viewmodel.HomeViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import androidx.activity.enableEdgeToEdge
import com.kakao.sdk.common.KakaoSdk
import com.peachspot.liteum.util.Logger

///@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var backPressedTime: Long = 0
    private val exitToastDuration: Int = 2000 // 2초 (밀리초 단위)
    private val TAG = "liteum"
    // Composable에서 다이얼로그 표시 여부를 제어하기 위한 상태
    private var showUpdateDialogState by mutableStateOf(false)
    private var updateInfoState by mutableStateOf<UpdateInfo?>(null)
    private var showServiceStoppedDialogState by mutableStateOf(false) // 서비스 중지 다이얼로그 상태
    private var showForegroundNotificationDialogState by mutableStateOf(false)
    private var foregroundNotificationDataState by mutableStateOf<Map<String, String>?>(null)
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var imageUri: Uri? = null
    private val FILE_REQUEST_CODE = 1001
    private lateinit var fileChooserLauncher: ActivityResultLauncher<Intent>

    // 앱 생명주기 상태 추적
    private var isAppInBackground = false
    private var wasInBackground = false

    data class UpdateInfo(
        val isForceUpdate: Boolean,
        val message: String,
        val storeUrl: String
    )

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Logger.d(TAG, "POST_NOTIFICATIONS permission granted.")
                // 권한이 허용된 경우 처리 (예: FCM 토큰 가져오기 등)
            } else {
                Logger.d(TAG, "POST_NOTIFICATIONS permission denied.")
                Toast.makeText(this, "알림 권한이 거부되었습니다. 일부 기능이 제한될 수 있습니다.", Toast.LENGTH_LONG).show()
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
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        FirebaseApp.initializeApp(this)
        Firebase.appCheck.installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )
        setupRemoteConfig()
        KakaoSdk.init(this, getString(R.string.kakao_app_key))

        fileChooserLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val resultUri = result.data?.data ?: imageUri
            if (result.resultCode == RESULT_OK && resultUri != null) {
                filePathCallback?.onReceiveValue(arrayOf(resultUri))
            } else {
                filePathCallback?.onReceiveValue(null)
            }
            filePathCallback = null
        }

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
            liteumiTheme {
                val navController = rememberNavController()
                val context = LocalContext.current.applicationContext as Application
                val database = remember { AppDatabase.getInstance(context) }
                val userPrefs = remember { UserPreferencesRepository(context) }
                val firebase = remember { FirebaseAuth.getInstance() }
                val repository = remember { HomeRepositoryImpl(database.bookLogsDao()) }

                val viewModelFactory = remember {
                    HomeViewModelFactory(
                        context,
                        userPrefs,
                        firebase,
                        myApiService,
                        repository

                    )
                }
                val homeViewModel: HomeViewModel = viewModel(factory = viewModelFactory)

//                // 백그라운드에서 돌아왔을 때 웹뷰 새로고침 트리거
//                LaunchedEffect(wasInBackground) {
//                    if (wasInBackground) {
//                        //homeViewModel.refreshWebViewsAfterBackground()
//                        wasInBackground = false
//                    }
//                }

                MainScreen(
                    navController = navController,
                    homeViewModel = homeViewModel,
                    onFileChooserRequest = { callback, intent ->
                        filePathCallback = callback
                        imageUri = createImageUri()

                        imageUri?.let { uri ->
                            val chooserIntent = createCameraGalleryChooserIntent(this, uri)
                            fileChooserLauncher.launch(chooserIntent)
                        } ?: run {
                            fileChooserLauncher.launch(intent)
                        }
                    }
                )

                LaunchedEffect(Unit) {
                    if (!isNetworkAvailable()) {
                        showServiceStoppedDialogState=true
                    } else {
                        checkAppVersion()
                    }
                }

                // 서비스 중지 다이얼로그
                if (showServiceStoppedDialogState) {
                    ShowServiceStoppedDialogComposable(
                        onConfirm = {
                            finishAffinity() // 앱 완전 종료
                        }
                    )
                }

                // 업데이트 다이얼로그 Composable 호출 (서비스 중지 다이얼로그가 아닐 때만)
                if (!showServiceStoppedDialogState) {
                    updateInfoState?.let { info ->
                        ShowUpdateDialogComposable(
                            showDialog = showUpdateDialogState,
                            onDismissRequest = {
                                if (!info.isForceUpdate) {
                                    showUpdateDialogState = false
                                }
                            },
                            isForceUpdate = info.isForceUpdate,
                            message = info.message,
                            storeUrl = info.storeUrl,
                            onUpdateClick = {
                                try {
                                    startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse(info.storeUrl)
                                        )
                                    )
                                } catch (e: ActivityNotFoundException) {
                                    startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                                        )
                                    )
                                }
                                finishAffinity()
                            }
                        )
                    }
                }

                // 포그라운드 알림 다이얼로그
                if (showForegroundNotificationDialogState) {
                    foregroundNotificationDataState?.let { data ->
                        ShowForegroundNotificationDialog(
                            title = data["title"] ?: "알림",
                            message = data["body"] ?: "새로운 메시지가 도착했습니다.",
                            link = data["link"],
                            onDismiss = { showForegroundNotificationDialogState = false },
                            onConfirm = {
                                showForegroundNotificationDialogState = false
                                data["link"]?.takeIf { it.isNotBlank() }?.let { url ->
                                    try {
                                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    } catch (e: ActivityNotFoundException) {
                                        Log.e(TAG, "Failed to open link: $url", e)
                                        Toast.makeText(
                                            this@MainActivity,
                                            "연결된 앱을 찾을 수 없습니다.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        requestNotificationPermissionIfNeeded()
    }

    override fun onStart() {
        super.onStart()
        Logger.d(TAG, "onStart called")

        LocalBroadcastManager.getInstance(this).registerReceiver(
            foregroundMessageReceiver,
            IntentFilter(MyFirebaseMessagingService.ACTION_FOREGROUND_MESSAGE)
        )

        // 백그라운드에서 돌아온 경우 플래그 설정
        if (isAppInBackground) {
            wasInBackground = true
            isAppInBackground = false
            Logger.d(TAG, "App returned from background")
        }
    }

    override fun onStop() {
        super.onStop()
        Logger.d(TAG, "onStop called")
        isAppInBackground = true
        LocalBroadcastManager.getInstance(this).unregisterReceiver(foregroundMessageReceiver)
    }

    override fun onResume() {
        super.onResume()
        Logger.d(TAG, "onResume called")
    }

    override fun onPause() {
        super.onPause()
        Logger.d(TAG, "onPause called")
    }

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
        const val ACTION_FOREGROUND_MESSAGE = "com.peachspot.liteum.ACTION_FOREGROUND_MESSAGE"
        const val EXTRA_TITLE = "com.peachspot.liteum.EXTRA_TITLE"
        const val EXTRA_BODY = "com.peachspot.liteum.EXTRA_BODY"
        const val EXTRA_LINK = "com.peachspot.liteum.EXTRA_LINK"
    }

    private fun setupRemoteConfig() {
        val remoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds =
                if (com.peachspot.liteum.BuildConfig.DEBUG) {
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
                    Log.e(TAG, "Failed to load Remote Config defaults.", task.exception)
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
                    if (getServiceRunning == false) {
                        showServiceStoppedDialogState = true
                    } else {
                        val latestVersionCode = remoteConfig.getLong("latest_version_code")
                        val isForceUpdate = remoteConfig.getBoolean("is_force_update")
                        val updateMessage = remoteConfig.getString("update_message")
                        val storeUrl = remoteConfig.getString("store_url")
                        val currentVersionCode = com.peachspot.liteum.BuildConfig.VERSION_CODE
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
                    Log.e(TAG, "Failed to fetch remote config.", task.exception)
                }
            }
    }

    @Composable
    private fun ShowUpdateDialogComposable(
        showDialog: Boolean,
        onDismissRequest: () -> Unit,
        isForceUpdate: Boolean,
        message: String,
        storeUrl: String,
        onUpdateClick: () -> Unit
    ) {
        if (showDialog) {
            AlertDialog(
                onDismissRequest = {
                    if (!isForceUpdate) {
                        onDismissRequest()
                    }
                },
                title = {
                    Text(text = getString(R.string.update_available_title))
                },
                text = {
                    Text(text = message.ifEmpty { getString(R.string.default_update_message) })
                },
                confirmButton = {
                    TextButton(onClick = onUpdateClick) {
                        Text(getString(R.string.update_button))
                    }
                },
                dismissButton = {
                    if (!isForceUpdate) {
                        TextButton(onClick = onDismissRequest) {
                            Text(getString(R.string.later_button))
                        }
                    }
                }
            )
        }
    }

    @Composable
    private fun ShowServiceStoppedDialogComposable(
        onConfirm: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text(text = stringResource(R.string.exitDialogTitle))
            },
            text = {
                Text(text = stringResource(R.string.exitDialogMent))
            },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text("확인")
                }
            }
        )
    }

    @Composable
    private fun ShowForegroundNotificationDialog(
        title: String,
        message: String,
        link: String?,
        onDismiss: () -> Unit,
        onConfirm: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(text = title) },
            text = { Text(text = message) },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text(if (link.isNullOrBlank()) "확인" else "이동")
                }
            },
            dismissButton = {
                if (link.isNullOrBlank()) {
                    TextButton(onClick = onDismiss) {
                        Text("닫기")
                    }
                }
            }
        )
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

    private fun createImageUri(): Uri {
        val imageFile = File(cacheDir, "IMG_${System.currentTimeMillis()}.png")
        return FileProvider.getUriForFile(
            this,
            "$packageName.fileprovider",
            imageFile
        )
    }

    fun createCameraGalleryChooserIntent(context: Context, outputUri: Uri): Intent {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, outputUri)
        }

        val galleryIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
        }

        return Intent(Intent.ACTION_CHOOSER).apply {
            putExtra(Intent.EXTRA_INTENT, galleryIntent)
            putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))
        }
    }
}