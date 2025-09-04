package com.peachspot.legendkofarm.ui.components

import android.view.SoundEffectConstants
import android.widget.Toast
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext


@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun MyAppTopBar(
    title: String = "",
    //isNotificationOn: Boolean,
    //onNotificationToggle: () -> Unit,
    onNotificationClick: () -> Unit = {},
    onTitleClick: () -> Unit,
    onMenuClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current

    TopAppBar(
        title = {
//            TextButton(onClick = onTitleClick) {
//                Text(text = title, color = Color.White)
//            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF2F2F2F).copy(alpha = 0.5f), // 반투명
            titleContentColor = Color.White
        ),
        navigationIcon = {
            if (onMenuClick != null) {
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Default.Menu, contentDescription = "메뉴", tint = Color.White)
                }
            }
        },
        actions = {
            IconButton(
                onClick = {
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                    //onNotificationToggle()
                    onNotificationClick()
                },
                modifier = Modifier.fillMaxHeight()
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "알림",
                    //tint = if (isNotificationOn) Color(0xFF81C784) else Color.White
                    tint =  Color.White
                )
            }
        },
        modifier = modifier.height(80.dp)
    )
}
