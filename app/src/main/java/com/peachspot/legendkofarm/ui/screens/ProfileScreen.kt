package com.peachspot.legendkofarm.ui.screens


import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.peachspot.legendkofarm.R
import com.peachspot.legendkofarm.ui.components.ConfirmationDialog
import com.peachspot.legendkofarm.ui.components.HomeTopAppBar
import com.peachspot.legendkofarm.ui.components.PrivacyPolicyLink
import com.peachspot.legendkofarm.util.Logger

import com.peachspot.legendkofarm.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    viewModel: HomeViewModel,
    navController: NavController, // NavController를 파라미터로 받도록 수정
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showTermsDialog by rememberSaveable { mutableStateOf(false) } // 약관 다이얼로그 표시 상태
    var showExitDialog by rememberSaveable { mutableStateOf(false) } // 탈퇴
    var saveDataDialog by rememberSaveable { mutableStateOf(false) } // 데이터 저장
    var loadDataDialog by rememberSaveable { mutableStateOf(false) } // 데이터 불러오기
    val focusManager = LocalFocusManager.current // LocalFocusManager 인스턴스 가져오기
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)


    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.handleSignInActivityResult(result.data)//Logger.d("ProfileScreen", "로그인 성공")
        } else {
            viewModel.clearSignInPendingIntent()//Logger.w("ProfileScreen", "로그인 실패 또는 취소: ${result.resultCode}")
        }
    }

    LaunchedEffect(uiState.signInPendingIntent) {
        uiState.signInPendingIntent?.let { intentSender ->
            try {
                signInLauncher.launch(
                    IntentSenderRequest.Builder(intentSender).build()
                )    //Logger.d("ProfileScreen", "로그인 인텐트 실행")
                viewModel.onSignInLaunched()
            } catch (e: Exception) {
                viewModel.clearSignInPendingIntent()
                snackbarHostState.showSnackbar("로그인 시작 오류: ${e.localizedMessage}")  //Logger.e("ProfileScreen", "인텐트 실행 실패", e)
            }
        }
    }  // 수정된 부분: uiState.userMessage 관찰
    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short // 또는 Long
            )
            viewModel.clearUserMessage() // Snackbar가 표시된 후 ViewModel에서 메시지 지우기
        }
    }
    // --- 여기부터 수정 ---
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    fun website() {
        val url =
            "https://www.peachspot.co.kr/legendkofarm" // 여기에 실제 웹사이트 주소 입력
        try {
            uriHandler.openUri(url)
        } catch (e: Exception) {
            // URL 열기 실패 시 사용자에게 알림 (예: Toast 메시지)
            // Toast.makeText(context, "웹사이트를 열 수 없습니다.", Toast.LENGTH_SHORT).show()
            Logger.e("ProfileScreen", "Failed to open URL: $url", e)
        }
    }


    fun privacy() {
        val url =
            "https://www.peachspot.co.kr/legendkofarm/privacy" // 여기에 실제 웹사이트 주소 입력
        try {
            uriHandler.openUri(url)
        } catch (e: Exception) {
            // URL 열기 실패 시 사용자에게 알림 (예: Toast 메시지)
            // Toast.makeText(context, "웹사이트를 열 수 없습니다.", Toast.LENGTH_SHORT).show()
            Logger.e("ProfileScreen", "Failed to open URL: $url", e)
        }
    }



    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("오르기 legendkofarm ", style = MaterialTheme.typography.titleMedium)
                    Text("올라가는 운동을 위한\n기록용 앱입니다.\n운동기록을 솔직하게 기록하시면\n됩니다.")
                    Spacer(modifier = Modifier.height(8.dp))


                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                Icons.Filled.Home, // 예시 아이콘
                                contentDescription = "웹사이트"
                            )
                        },
                        label = { Text("웹사이트") },
                        selected = false, // 실제 선택 상태에 따라 동적으로 변경
                        onClick = {
                            website()
                            scope.launch { drawerState.close() }
                        }
                    )
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                Icons.Filled.Home, // 예시 아이콘
                                contentDescription = "개인정보취급방침"
                            )
                        },
                        label = { Text("개인정보취급방침") },
                        selected = false, // 실제 선택 상태에 따라 동적으로 변경
                        onClick = {
                            privacy()
                            scope.launch { drawerState.close() }
                        }
                    )

                    // 다른 메뉴 항목들...
                }
            }
        }
    ) {
        Scaffold(
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    snackbar = { snackbarData ->
                        SnackbarHost(hostState = snackbarHostState) { snackbarData ->
                            // 현재 uiState의 userMessageType 또는 snackbarData의 메시지 내용을 기반으로 색상 결정
                            val isErrorMessage =
                                snackbarData.visuals.message == uiState.userMessage && uiState.userMessage != null
                            val containerColor = when (uiState.userMessageType) {
                                "info" -> Color(0xFF4CAF50)
                                "success" -> Color(0xFF4CAF50) // 성공 메시지 색상 (예시)
                                "error" -> Color(0xFFFF0000) // 성공 메시지 색상 (예시)
                                else -> {
                                    Color(0xFFCCCCCC)
                                }
                            }
                            Snackbar(
                                snackbarData = snackbarData,
                                containerColor = containerColor,
                                contentColor = Color(0xFFFFFFFF)//contentColor,
                            )
                        }
                    }
                )
            },
            topBar = {
                HomeTopAppBar(
                    title = stringResource(R.string.screen_title),
                    onMenuClick = { // HomeTopAppBar에 onMenuClick 콜백이 있어야 함
                        scope.launch {
                            if (drawerState.isClosed) {
                                drawerState.open()
                            } else {
                                drawerState.close()
                            }
                        }
                    })
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()), // 세로 스크롤 추가
                //.padding(16.dp), // 전체적인 내부 패딩 추가
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top // 여기를 Top으로 변경
            ) {
                Spacer(Modifier.height(40.dp)) // 하단 여백



                Text(
                    text = stringResource(R.string.profile_google_sync_description),
                    style = TextStyle(
                        color = Color(0xFFCCCCCC),
                        fontSize = 16.sp // Example: Set font size as well
                    )
                )

                Spacer(Modifier.height(24.dp)) // 상단 여백
                when {
                    uiState.isLoading -> CircularProgressIndicator()
                    uiState.isUserLoggedIn -> LoggedInUserProfile(
                        uiState,
                        onSignOutClicked = { viewModel.signOut() }
                    )
                    // 로그인 버튼은 약관 동의 시에만 활성화 (선택적 구현)
                    else -> LoginPrompt(
                        onSignInClicked = {

                            if (uiState.termsAccepted == true) {
                                viewModel.startGoogleSignIn()
                            } else {

                                viewModel.needAgree()

                            }
                        },
                        //  enabled = uiState.termsAccepted // 체크박스 상태에 따라 버튼 활성화/비활성화
                    )
                }

                if (!uiState.isUserLoggedIn) {
                    Spacer(Modifier.height(16.dp)) // 상단 여백
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(bottom = 0.dp) // TextButton 패딩이 조정되면 이것을 제거해 볼 수 있습니다.
                            .clickable { viewModel.toggleTermsAccepted() }
                    ) {
                        Checkbox(
                            checked = uiState.termsAccepted, // ViewModel의 상태 사용
                            onCheckedChange = { accepted ->
                                viewModel.toggleTermsAccepted()
                            }
                        )

                        Text(
                            text = stringResource(R.string.profile_agree_to_privacy_policy),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            modifier = Modifier
                                .padding(4.dp) // Optional: Add some padding inside the border so text isn't too close
                        )
                    }

                    PrivacyPolicyLink()
                }




                if (showTermsDialog) {
                    TermsDialog(onDismiss = { showTermsDialog = false })
                }


                Spacer(Modifier.height(24.dp)) // 상단 여백

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                ) {
                    if (uiState.isUserLoggedIn) {
//                        TextButton(
//                            onClick = { saveDataDialog = true },
//                            enabled = uiState.isUserLoggedIn
//                        ) {
//                            Text("[저장하기]")
//                        }
//
//                        TextButton(
//                            onClick = { loadDataDialog = true },
//                            enabled = uiState.isUserLoggedIn
//                        ) {
//                            Text("[가져오기]")
//                        }

                        TextButton(
                            onClick = { showExitDialog = true },
                            enabled = uiState.isUserLoggedIn
                        ) {
                            Text("[계정 삭제]")
                        }
                    }
                }


                if (showExitDialog) {
                    ConfirmationDialog(
                        title = "계정 삭제",
                        text = " 저장된 데이터가 모두 삭제 됩니다.",
                        onDismiss = { showExitDialog = false },
                        onConfirm = { viewModel.deleteUserAccount() },
                        confirmButtonText = "삭제"
                    )

                }


            }
        }

    }
}



@Composable
fun Icon(
    imageVector: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified
) {
    androidx.compose.material3.Icon( // 실제 아이콘을 그리는 표준 Icon 컴포저블 사용
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint
    )
}



@Composable
fun TermsDialog(onDismiss: () -> Unit) {
    // 실제 약관 내용은 여기에 긴 문자열로 넣거나,
    // 별도의 리소스 파일에서 불러오거나, 스크롤 가능한 형태로 구성할 수 있습니다.
    val termsAndConditionsText =
        stringResource(R.string.privacy_policy_content) // Use string resource

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("이용약관") },
        text = {
            // 약관 내용이 길 경우 스크롤 가능하도록 Column과 verticalScroll 사용
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(termsAndConditionsText, style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_close))
            }
        }
    )
}

