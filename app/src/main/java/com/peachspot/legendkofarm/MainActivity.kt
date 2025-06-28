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
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.peachspot.legendkofarm.data.db.AppDatabase
import com.peachspot.legendkofarm.data.remote.client.NetworkClient.myApiService
import com.peachspot.legendkofarm.data.repositiory.HomeRepositoryImpl
import com.peachspot.legendkofarm.data.repositiory.UserPreferencesRepository
import com.peachspot.legendkofarm.services.MyFirebaseMessagingService
import com.peachspot.legendkofarm.ui.navigation.AppScreenRoutes
import com.peachspot.legendkofarm.ui.screens.MainScreen
import com.peachspot.legendkofarm.ui.screens.NotificationScreen
import com.peachspot.legendkofarm.ui.theme.legendkofarmiTheme
import com.peachspot.legendkofarm.viewmodel.HomeViewModel
import com.peachspot.legendkofarm.viewmodel.HomeViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

///@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var backPressedTime: Long = 0
    private val exitToastDuration: Int = 2000 // 2초 (밀리초 단위)
    private val TAG = "legendkofarm"
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
    data class UpdateInfo(
        val isForceUpdate: Boolean,
        val message: String,
        val storeUrl: String
    )



    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.d(TAG, "POST_NOTIFICATIONS permission granted.")
                // 권한이 허용된 경우 처리 (예: FCM 토큰 가져오기 등)
            } else {
                Log.d(TAG, "POST_NOTIFICATIONS permission denied.")
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
        super.onCreate(savedInstanceState)
        // 다크모드 비활성화


        handleIntent(intent)
        FirebaseApp.initializeApp(this)
        Firebase.appCheck.installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )
           setupRemoteConfig()




        fileChooserLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            //val resultUri = result.data?.data
            val resultUri = result.data?.data ?: imageUri // <-- 중요!
            if (result.resultCode == RESULT_OK && resultUri != null) {
                filePathCallback?.onReceiveValue(arrayOf(resultUri))
            } else {
                filePathCallback?.onReceiveValue(null)
            }
            filePathCallback = null
        }





        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (System.currentTimeMillis() - backPressedTime < exitToastDuration) {
                    finish()
                } else {
                    Toast.makeText(this@MainActivity, "빠르게 한 번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT)
                        .show()
                    backPressedTime = System.currentTimeMillis()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        setContent {
            legendkofarmiTheme {

                val application = LocalContext.current.applicationContext as Application
                val navController = rememberNavController()
                val context = LocalContext.current.applicationContext as Application
                val database = remember { AppDatabase.getInstance(context) }
                val userPrefs = remember { UserPreferencesRepository(context) }
                val firebase = remember { FirebaseAuth.getInstance() }
                val repository = remember { HomeRepositoryImpl(database.farmLogDao()) }

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
                                if (info.isForceUpdate) {
                                    finishAffinity()
                                } else {
                                    showUpdateDialogState = false
                                }
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

            LaunchedEffect(Unit) {
                checkAppVersion()
                ////registerAppToken()
            }


        }

        ///requestLocationPermissionIfNeeded()
        requestNotificationPermissionIfNeeded() // 알림 권한 요청 함수 호출  알림 권한 받고나서야 위치권한 받게 하려면?
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
                // 이더넷 연결 등 다른 유형의 네트워크도 확인할 수 있습니다.
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                // 블루투스 인터넷 테더링 등
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
                else -> false
            }
        } else {
            // API 23 미만 버전 (deprecated)
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return networkInfo.isConnected
        }
    }


    override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            foregroundMessageReceiver,
            IntentFilter(MyFirebaseMessagingService.ACTION_FOREGROUND_MESSAGE)
        )
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(foregroundMessageReceiver)
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13 (API 33) 이상
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d(TAG, "POST_NOTIFICATIONS permission already granted.")
                }

                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    Log.d(TAG, "Showing rationale for POST_NOTIFICATIONS permission.")
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }

                else -> {
                    Log.d(TAG, "Requesting POST_NOTIFICATIONS permission.")
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            Log.d(
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
        val configSettings = remoteConfigSettings {
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
                    Log.d(TAG, "Remote Config defaults loaded.")
                } else {
                    Log.e(TAG, "Failed to load Remote Config defaults.", task.exception)
                }
            }
    }

//    private fun registerAppToken() {
//
//        FirebaseMessaging.getInstance().getToken()
//            .addOnCompleteListener(object : OnCompleteListener<String?> {
//                public override fun onComplete(task: Task<String?>) {
//                    // Get new FCM registration token
//                    val token: String? = task.getResult()
//                    this@MainActivity.send_token(token)
//                }
//            })
//    }

//    fun send_token(token: String?) {
//        if (token?.isEmpty() == true) {
//            return
//        }
//
//        CoroutineScope(Dispatchers.IO).launch {
//            try {
//                val AppToken = mapOf("token" to token)
//                val response = myApiService.registerDevice("AppToken", token.toString())
//            } catch (e: Exception) {
//                Log.e("Main", "Exception while sending token to server.", e)
//            }
//        }
//    }

//
    private fun checkAppVersion() {
        val remoteConfig = Firebase.remoteConfig
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val updated = task.result
                    val getServiceRunning = remoteConfig.getBoolean("running") // 키 이름 확인 필요
                    if (getServiceRunning == false) {
                        // 서비스가 중지된 경우
                        showServiceStoppedDialogState = true
                    } else {
                        // 서비스가 실행 중인 경우, 기존 버전 체크 로직 수행
                        val latestVersionCode = remoteConfig.getLong("latest_version_code")
                        val isForceUpdate = remoteConfig.getBoolean("is_force_update")
                        val updateMessage = remoteConfig.getString("update_message")
                        val storeUrl = remoteConfig.getString("store_url")
                        val currentVersionCode = com.peachspot.legendkofarm.BuildConfig.VERSION_CODE
                        Log.d(
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
                            Log.d(TAG, "App is up to date.")
                        }
                    }
                } else {
                    Log.e(TAG, "Failed to fetch remote config.", task.exception)
                    // 실패 시 기본적으로 서비스가 실행 중이라고 가정하거나,
                    // 또는 네트워크 오류 등의 메시지를 표시할 수 있습니다.
                    // 여기서는 일단 기존 로직대로 둡니다.
                }
            }
    }

//
@Composable
fun AppNavHost(
    onFileChooserRequest: (ValueCallback<Array<Uri>>, Intent) -> Unit
) {
    val navController = rememberNavController()
    val context = LocalContext.current.applicationContext as Application
    val database = remember { AppDatabase.getInstance(context) }
    val userPrefs = remember { UserPreferencesRepository(context) }
    val firebase = remember { FirebaseAuth.getInstance() }
    val repository = remember { HomeRepositoryImpl(database.farmLogDao()) }

    val viewModelFactory = remember {
        com.peachspot.legendkofarm.viewmodel.HomeViewModelFactory(
            context,
            userPrefs,
            firebase,
            myApiService,
            repository
        )
    }
    val homeViewModel: HomeViewModel = viewModel(factory = viewModelFactory)

    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScreen(
                navController = navController,
                homeViewModel = homeViewModel,
                onFileChooserRequest = onFileChooserRequest
            )
        }

        // 탭 외부 화면
        composable(AppScreenRoutes.NOTIFICATION_SCREEN) {
            NotificationScreen(
                onNavigateBack = { navController.popBackStack() }
            )
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


    // 서비스 중지 알림 다이얼로그 Composable
    @Composable
    private fun ShowServiceStoppedDialogComposable(
        onConfirm: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = { /* 강제 종료이므로 닫기 동작 없음 */ },
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
                if (link.isNullOrBlank()) { // 링크가 없을 때만 닫기 버튼 표시
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
                Log.d("MainActivity", "Link from notification: $link")
                // 여기서 link를 사용하여 WebView를 로드하거나 다른 동작을 수행
                // 예: showWebViewDialog(link) 또는 navigateToLink(link)
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