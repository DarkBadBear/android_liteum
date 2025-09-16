package com.peachspot.legendkofarm.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.peachspot.legendkofarm.ui.components.MyAppTopBar
import com.peachspot.legendkofarm.viewmodel.HomeViewModel
import com.peachspot.legendkofarm.ui.components.CommonWebView
import com.peachspot.legendkofarm.R
import com.peachspot.legendkofarm.data.db.NotificationEntity
import com.peachspot.legendkofarm.ui.navigation.AppScreenRoutes
import com.peachspot.legendkofarm.viewmodel.NotificationViewModel
import com.peachspot.legendkofarm.viewmodel.NotificationViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.peachspot.legendkofarm.util.Logger


object HomeScreenContentTypes {
    const val INFO = "info"
    const val SUCCESS = "success"
    const val ERROR = "error"
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    navController: NavController,
    modifier: Modifier = Modifier,
    onFileChooserRequest: (ValueCallback<Array<Uri>>, Intent) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val leftDrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val rightDrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    val application = LocalContext.current.applicationContext as Application
    val notificationViewModel: NotificationViewModel = viewModel(
        factory = NotificationViewModelFactory(application)
    )
    val notifications by notificationViewModel.notifications.collectAsState()


    LaunchedEffect(cameraPermissionState.status) {
        if (!cameraPermissionState.status.isGranted) {
            Logger.d("DiaryScreen", "Requesting camera permission")
            cameraPermissionState.launchPermissionRequest()
        }
    }



    // Snackbar 메시지 처리
    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearUserMessage()
        }
    }

    // 로그인 상태 변화 감지 → 서버 리다이렉트 로그인 시도
    LaunchedEffect(uiState.webViewAuthUrl, uiState.firebaseUid, uiState.kakaoUid,uiState.userEmail) {
        val url = uiState.webViewAuthUrl
        val headers = mutableMapOf<String, String>()
        uiState.firebaseUid?.let { headers["X-Firebase-Uid"] = it }
        uiState.kakaoUid?.let { headers["X-Kakao-Uid"] = it }
        uiState.userEmail?.let { headers["X-email"] = it }

        if (!url.isNullOrBlank() && headers.isNotEmpty()) {
            val webView = viewModel.getOrCreateWebView(
                context = context,
                tag = "home",
                url = url
            )
            viewModel.loadUrlWithHeaders(webView, url, headers)
        }
    }

    // 앱 종료 상태 감지
    LaunchedEffect(uiState.isEnding) {
        if (uiState.isEnding) {
            navController.navigate("byebye") {
                launchSingleTop = true
            }
        }
    }


    // BackHandler - Drawer 닫기
    if (leftDrawerState.isOpen) {
        BackHandler { scope.launch { leftDrawerState.close() } }
    }
    if (rightDrawerState.isOpen) {
        BackHandler { scope.launch { rightDrawerState.close() } }
    }

    // 왼쪽 Drawer
    val leftDrawerContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                SmallFloatingActionButton(
                    onClick = { scope.launch { leftDrawerState.close() } },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier.align(Alignment.CenterEnd) // 👉 오른쪽 끝으로 정렬
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "닫기",
                        modifier = Modifier.size(18.dp) // 아이콘도 같이 줄이기
                    )
                }
            }


            Spacer(Modifier.height(10.dp))
            Image(
                painter = painterResource(id = R.drawable.round_legendkofarm),
                contentDescription = "App Logo",
                modifier = Modifier.size(200.dp)
            )
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    scope.launch {
                        leftDrawerState.close()
                        val url = "https://peachspot.co.kr/blog/detail?no=2"
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFefefef),
                    contentColor = Color.Black
                ),
                modifier = Modifier.width(200.dp) // 원하는 너비로 설정
            ) { Text("웹사이트") }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    scope.launch {
                        leftDrawerState.close()
                        val url = "https://peachspot.co.kr/privacy?no=2"
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFefefef),
                    contentColor = Color.Black
                ),
                modifier = Modifier.width(200.dp) // 원하는 너비로 설정
            ) { Text("개인정보취급방침") }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    scope.launch {
                        leftDrawerState.close()
                        viewModel.logOut() // <- 상태 변경만 수행
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFefefef),
                    contentColor = Color.Black
                ),
                modifier = Modifier.width(200.dp) // 원하는 너비로 설정
            ) { Text("로그아웃") }

            Spacer(Modifier.height(24.dp))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = { expanded = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFefefef),
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.width(200.dp) // 원하는 너비로 설정
                    ) { Text("계정 관리") }

                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(text = { Text("계정 삭제") }, onClick = {
                            expanded = false
                            // 계정 삭제 처리
                        })
                    }
                }
            }


            // 이 부분을 추가합니다.
            Spacer(Modifier.weight(1f)) // 사용 가능한 모든 공간을 차지하도록 합니다.
            Text(
                text = "Powered by Peachspot",
                style = MaterialTheme.typography.bodySmall, // 또는 원하는 스타일
                color = Color.Gray, // 또는 원하는 색상
                modifier = Modifier.padding(bottom = 16.dp) // 하단 여백 추가 (선택 사항)
            )
            // 여기까지 추가합니다.

        }
    }

    // 오른쪽 알림 Drawer
    val rightDrawerContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "알림",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    if (notifications.isNotEmpty()) {
                        IconButton(onClick = { notificationViewModel.clearNotifications() }) {
                            Icon(Icons.Filled.Delete, contentDescription = "모든 알림 삭제")
                        }
                    }
                    IconButton(onClick = { scope.launch { rightDrawerState.close() } }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "닫기")
                    }
                }
            }
            Divider()
            if (notifications.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("알림이 없습니다")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(notifications, key = { it.id }) { notification ->
                        NotificationItem(notification = notification)
                    }
                }
            }
        }
    }

    // Drawer & Scaffold
    ModalNavigationDrawer(
        drawerContent = { if (uiState.isUserLoggedIn) rightDrawerContent() },
        drawerState = rightDrawerState,
        gesturesEnabled = false
    ) {
        ModalNavigationDrawer(
            drawerContent = { if (uiState.isUserLoggedIn) leftDrawerContent() },
            drawerState = leftDrawerState,
            gesturesEnabled = false
        ) {
            Scaffold(
                //contentWindowInsets = WindowInsets(0),
                containerColor = Color.White,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    MyAppTopBar(
                        title = stringResource(R.string.screen_title),
                        onNotificationClick = { scope.launch { if (rightDrawerState.isClosed) rightDrawerState.open() else rightDrawerState.close() } },
                        onTitleClick = {},
                        onZoomInClick = {
                            viewModel.zoomInActiveWebView() // View
                        },
                        onZoomOutClick = {
                            viewModel.zoomOutActiveWebView() //
                        },
                        onMenuClick = if (uiState.isUserLoggedIn) {
                            { scope.launch { if (leftDrawerState.isClosed) leftDrawerState.open() else leftDrawerState.close() } }
                        } else null
                    )
                }
            ) { innerPadding ->
                Box(modifier = modifier.padding(innerPadding).fillMaxSize()) {
                    val webView = viewModel.getOrCreateWebView(
                        context = context,
                        tag = "home",
                        url = "https://peachspot.co.kr/lkf?uid={${uiState.firebaseUid}",
                    )

                    CommonWebView(
                        webView = webView,
                        modifier = modifier.fillMaxSize(),
                        onShowPopup = { url, webView ->
                            // 팝업 처리
                        },
                        onJsAlert = { message, result ->
                            // JS Alert 처리
                        },
                        onFileChooserRequest = onFileChooserRequest
                    )
                }
            }
        }
    }
}
@Composable
fun NotificationItem(notification: NotificationEntity, modifier: Modifier = Modifier) {
    val dateFormat = remember { SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault()) }
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            notification.imgUrl?.takeIf { it.isNotBlank() }?.let { imageUrl ->
                AsyncImage(
                    model = imageUrl,
                    contentDescription = notification.title ?: "알림 이미지",
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            notification.title?.let { title ->
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            notification.body?.let { body ->
                Text(text = body, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text(
                text = dateFormat.format(Date(notification.receivedTimeMillis)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

