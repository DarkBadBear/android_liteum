package com.peachspot.legendkofarm.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavHostController

import com.peachspot.legendkofarm.R


import com.peachspot.legendkofarm.util.Logger
import com.peachspot.legendkofarm.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    viewModel: HomeViewModel,
    navController: NavController,
    onLoginSuccess: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope() // 여기서 scope 정의
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // 로그인 상태 변화를 감지
    LaunchedEffect(uiState.isUserLoggedIn) {
        if (uiState.isUserLoggedIn) onLoginSuccess()
    }

    // 로딩 화면 이동
    LaunchedEffect(uiState.isLoading) {
        if (uiState.isLoading) {
            navController.navigate("loading") { launchSingleTop = true }
        }
    }

    // 메시지 처리
    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            Logger.d("LoginScreen", "Snackbar show: $msg")
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        containerColor = Color.White,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Powered by Peachspot",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(
                    start = 10.dp,
                    end = 10.dp,
                    top = 30.dp,
                    bottom = 30.dp
                ),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 앱 로고
            Image(
                painter = painterResource(id = R.drawable.legendkofarm),
                contentDescription = "App Logo",
                modifier = Modifier.size(200.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))
            Text("전설의 농부", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(10.dp))
            Text("서비스를 이용하려면 로그인해주세요.", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(30.dp))

            Image(
                painter = painterResource(id = R.drawable.login_kakao),
                contentDescription = "Google Sign-In",
                modifier = Modifier
                    .clickable(
                        // 리플 효과 제거
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        (context as? Activity)?.let {
                                activity ->viewModel.startKakaoSignIn(activity)
                        } ?: run {
                            Logger.e("LoginScreen", "Context is not an Activity, cannot start kakao.")
                        }
                    }
                    .padding(1.dp)
                    .size(width = 150.dp, height = 30.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Image(
                    painter = painterResource(id = R.drawable.login_google),
                    contentDescription = "Google Sign-In",
                    modifier = Modifier
                        .clickable(
                            // 리플 효과 제거
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            (context as? Activity)?.let {
                                    activity ->viewModel.startGoogleSignIn()
                            } ?: run {
                                Logger.e("LoginScreen", "Context is not an Activity, cannot start Google Sign-In.")
                            }
                        }
                        .padding(1.dp)
                        .size(width = 120.dp, height = 30.dp)
                )
                Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = {
                    scope.launch {
                        val url = "https://peachspot.co.kr/blog/detail?no=2"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFefefef),
                    contentColor = Color.Black
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("서비스 소개",
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            // 개인정보취급방침
            Button(
                onClick = {
                    scope.launch {
                        val url = "https://peachspot.co.kr/privacy?no=2"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFefefef),
                    contentColor = Color.Black
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("개인정보취급방침",
                    fontSize = 12.sp)
            }


        }
    }
}







