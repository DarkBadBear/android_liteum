package com.peachspot.legendkofarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.peachspot.legendkofarm.data.core.ForegroundNotificationEvent
import com.peachspot.legendkofarm.data.core.NotificationEventBus
import com.peachspot.legendkofarm.data.db.AppDatabase
import com.peachspot.legendkofarm.data.db.NotificationEntity
import com.peachspot.legendkofarm.data.repository.HomeRepository
import com.peachspot.legendkofarm.util.Logger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var homeRepository: HomeRepository

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    val firebaseAuth = FirebaseAuth.getInstance()

    private val notificationDao by lazy {
        AppDatabase.getInstance(applicationContext).notificationDao()
    }

    companion object {
        private const val TAG = "legendkofarmMsgService"
        const val ACTION_FOREGROUND_MESSAGE = "com.peachspot.legendkofarm.ACTION_FOREGROUND_MESSAGE"
        const val EXTRA_TITLE = "com.peachspot.legendkofarm.EXTRA_TITLE"
        const val EXTRA_BODY = "com.peachspot.legendkofarm.EXTRA_BODY"
        const val EXTRA_LINK = "com.peachspot.legendkofarm.EXTRA_LINK"
        private const val KEY_SERVER_TITLE = "title"
        private const val KEY_SERVER_MESSAGE = "body"
        private const val KEY_SERVER_IMAGE = "image"
        private const val KEY_SERVER_LINK = "link"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Logger.d(TAG, "From: ${remoteMessage.from}")
        Logger.d(TAG, "Message ID: ${remoteMessage.messageId}")
        Logger.d(TAG, "Notification: ${remoteMessage.notification}")
        Logger.d(TAG, "Data: ${remoteMessage.data}")

        val data = remoteMessage.data
        var title: String? = null
        var message: String? = null
        var imageUrl: String? = null
        var link: String? = null

        if (data.isNotEmpty()) {
            title = data[KEY_SERVER_TITLE]
            message = data[KEY_SERVER_MESSAGE]
            imageUrl = data[KEY_SERVER_IMAGE]
            link = data[KEY_SERVER_LINK]
        }

        remoteMessage.notification?.let { notification ->
            if (title.isNullOrBlank()) title = notification.title
            if (message.isNullOrBlank()) message = notification.body
            if (imageUrl.isNullOrBlank()) imageUrl = notification.imageUrl?.toString()
        }

        if (title.isNullOrBlank() || message.isNullOrBlank()) {
            Logger.w(TAG, "Title or message is null or blank. Skipping notification.")
            return
        }

        val status = data["status"]
        val urlId = data["urlId"]?.toLongOrNull()

        serviceScope.launch {
            val imageBitmap = imageUrl?.let { downloadBitmapFromUrl(it) }
            sendNotification(title!!, message!!, imageBitmap, link)
        }

        if (status == "Error" && urlId != null) {
            // 1. 기존 로컬 브로드캐스트 발송 (Compose가 아닌 기존 UI 대응용)
            val intent = Intent("com.peachspot.ACTION_URL_STATUS_UPDATE").apply {
                putExtra("status", status)
                putExtra("urlId", urlId)
            }

            //LocalBroadcastManager.getInstance(this).sendBroadcast(intent)

            // 2. Compose용 SharedFlow 이벤트 발행
            serviceScope.launch {
                NotificationEventBus.events.emit(
                    ForegroundNotificationEvent(
                        title = title ?: "알림",
                        message = message ?: "",
                        urlId = urlId,
                        status = status
                    )
                )
            }
        }
    }

    private suspend fun downloadBitmapFromUrl(imageUrl: String): Bitmap? {
        if (imageUrl.isBlank()) return null
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(imageUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.connect()
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    connection.inputStream.buffered().use { input ->
                        BitmapFactory.decodeStream(input)
                    }
                } else {
                    Logger.e(TAG, "Failed to download image, code: ${connection.responseCode}")
                    null
                }
            } catch (e: Exception) {
                Logger.e(TAG, "Error downloading image: ${e.message}", e)
                null
            }
        }
    }

    private fun sendNotification(
        title: String,
        messageBody: String,
        imageBitmap: Bitmap?,
        link: String?
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            if (!link.isNullOrBlank()) {
                putExtra(EXTRA_LINK, link)
            }
        }

        val notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        val requestCode = notificationId
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            pendingIntentFlags
        )

        val channelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setVibrate(longArrayOf(500, 500))
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        imageBitmap?.let {
            notificationBuilder
                .setLargeIcon(it)
                .setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(it)
                        .bigLargeIcon(null as Bitmap?)
                )
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = getString(R.string.default_notification_channel_name)
            val channelDescription = getString(R.string.default_notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH

            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
                enableVibration(true)
                vibrationPattern = longArrayOf(500, 500)
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    override fun onNewToken(token: String) {
        Logger.d(TAG, "Refreshed token: $token")
        // 필요 시 서버에 토큰 등록하는 로직 구현
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}



