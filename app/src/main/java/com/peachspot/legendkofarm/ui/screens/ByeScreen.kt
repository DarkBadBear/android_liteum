package com.peachspot.legendkofarm.ui.screens

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.peachspot.legendkofarm.R
import kotlinx.coroutines.delay

@Composable
fun ByeScreen(message: String = "자리정리중... ") {

    val context = LocalContext.current
    // 1초 딜레이 후 앱 종료
    LaunchedEffect(Unit) {
        delay(1000L) // 1초 딜레이
        (context as? Activity)?.finish() // Activity 종료
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally

    ) {
        Spacer(Modifier.height(150.dp))

        // 로고
        Image(
            painter = painterResource(id = R.drawable.legendkofarm),
            contentDescription = "App Logo",
            modifier = Modifier.size(200.dp)
        )

        Spacer(Modifier.height(24.dp))



        Text(message)


    }
}
