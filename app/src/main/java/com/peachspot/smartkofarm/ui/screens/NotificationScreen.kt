package com.peachspot.smartkofarm.ui.screens

import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.peachspot.smartkofarm.R
import com.peachspot.smartkofarm.data.db.NotificationEntity
import com.peachspot.smartkofarm.viewmodel.NotificationViewModel
import com.peachspot.smartkofarm.viewmodel.NotificationViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onNavigateBack: () -> Unit
) {
    val application = LocalContext.current.applicationContext as Application
    // ViewModel 인스턴스 생성 (Hilt 미사용)
    val viewModel: NotificationViewModel = viewModel(
        factory = NotificationViewModelFactory(application)
    )
    val notifications by viewModel.notifications.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notifications_screen_title)) }, // strings.xml에 추가 필요
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back)
                        )
                    }
                },
                actions = {
                    if (notifications.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearNotifications() }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.clear_all_notifications)
                            ) // strings.xml에 추가 필요
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.no_notifications_available)) // strings.xml에 추가 필요
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
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

@Composable
fun NotificationItem(
    notification: NotificationEntity,
    modifier: Modifier = Modifier // Added modifier parameter
) {
    // Consider moving date formatting to a ViewModel or passing formatted string
    val dateFormat = remember { SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault()) }

    Card(
        modifier = modifier.fillMaxWidth(), // Used the passed modifier
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(

            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f) // Example
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline) // Using theme color
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp) // Standard padding
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Text content on the left
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp) // Spacing between text and image
            ) {
                if (notification.imgUrl != "") {
                    Row(
                        modifier = Modifier
                            .padding(16.dp) // Standard padding
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center, // Changed for centering
                        verticalAlignment = Alignment.CenterVertically // Good practice for vertical centering too
                    ) {
                        // Image on the right
                        notification.imgUrl?.takeIf { it.isNotBlank() }?.let { imageUrl ->
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = notification.title
                                    ?: stringResource(R.string.notification_item_image_content_desc),
                                modifier = Modifier
                                    .fillMaxWidth() // 1. Allow the image to potentially take the full width
                                    .widthIn(min = 80.dp) // 2. Enforce minimum width
                                    // No max width needed here if parent is already constraining or if fillMaxWidth is desired up to parent's bounds
                                    .aspectRatio(16f / 9f)
                                    .clip(RoundedCornerShape(8.dp)), // Optional: rounded corners for the image
                                contentScale = ContentScale.Crop, // Crop might look better if you want the image to fill the 16:9 bounds
//                                placeholder = painterResource(id = R.drawable.ic_image),
//                                error = painterResource(id = R.drawable.ic_no_image)ic_no_image
                            )
                        }
                    }
                }

                notification.title?.let { title ->
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 2, // Optional: prevent overly long titles
                        overflow = TextOverflow.Ellipsis // Optional: handle overflow
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                notification.body?.let { body ->
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodyMedium,
                    )
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
}
