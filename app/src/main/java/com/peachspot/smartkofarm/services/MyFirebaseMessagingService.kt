package com.peachspot.smartkofarm.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.peachspot.smartkofarm.MainActivity
import com.peachspot.smartkofarm.R
import com.peachspot.smartkofarm.data.db.AppDatabase
import com.peachspot.smartkofarm.data.db.NotificationEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL


class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private val notificationDao by lazy {
        AppDatabase.getInstance(applicationContext).notificationDao()
    }

    companion object {
        private const val TAG = "oroogiMsgService"
        const val ACTION_FOREGROUND_MESSAGE = "com.peachspot.oroogi.ACTION_FOREGROUND_MESSAGE"
        const val EXTRA_TITLE =
            "com.peachspot.oroogi.EXTRA_TITLE" // Consider moving these too if they are only used with this action
        const val EXTRA_BODY = "com.peachspot.oroogi.EXTRA_BODY"
        const val EXTRA_LINK = "com.peachspot.oroogi.EXTRA_LINK"
        private const val KEY_SERVER_TITLE = "title" // 예시: 서버에서 "title"로 보낸다면
        private const val KEY_SERVER_MESSAGE = "body"  // 예시: 서버에서 "body" 또는 "message"로 보낸다면
        private const val KEY_SERVER_IMAGE = "image" // 예시: 서버에서 "image"로 보낸다면
        private const val KEY_SERVER_LINK = "link"   // 예시: 서버에서 "link"로 보낸다면


    }

    //final BoobiDB dbManager = new BoobiDB(getApplicationContext(), "Boobischedule.db", null, 1);
    fun getBitmapfromUrl(imageUrl: String): Bitmap? {
        try {
            val url = URL(imageUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.setDoInput(true)
            connection.connect()
            val input = connection.getInputStream()
            val bitmap = BitmapFactory.decodeStream(input)
            return bitmap
        } catch (e: java.lang.Exception) {
            // TODO Auto-generated catch block
            e.printStackTrace()
            return null
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {

        Log.d(TAG, "From: ${remoteMessage.from}")
        Log.d(TAG, "Message ID: ${remoteMessage.messageId}")
        Log.d(TAG, "Notification: ${remoteMessage.notification}")
        Log.d(TAG, "Data: ${remoteMessage.data}")


        var title: String? = null
        var message: String? = null
        var imageUrl: String? = null
        var link: String? = null

        // 1. 데이터 페이로드에서 정보 추출 시도
        if (remoteMessage.data.isNotEmpty()) {
            title = remoteMessage.data[KEY_SERVER_TITLE]
            message = remoteMessage.data[KEY_SERVER_MESSAGE]
            imageUrl = remoteMessage.data[KEY_SERVER_IMAGE]
            link = remoteMessage.data[KEY_SERVER_LINK]
            Log.d(
                TAG,
                "Extracted from data payload: title='$title', message='$message', imageUrl='$imageUrl', link='$link'"
            )
        }

        // 2. 알림 페이로드에서 정보 추출 시도 (데이터 페이로드에 없거나, 알림 페이로드만 있는 경우)
        remoteMessage.notification?.let { notification ->
            if (title == null) title = notification.title
            if (message == null) message = notification.body
            if (imageUrl == null) imageUrl = notification.imageUrl?.toString()
//            Log.d(
//                TAG,
//                "Extracted from notification payload (if data was missing): title='${notification.title}', body='${notification.body}', imageUrl='${notification.imageUrl}'"
//            )

            if (title != null || message != null) {
                val notificationEntity = NotificationEntity(
                    title = title,
                    body = message,
                    receivedTimeMillis = System.currentTimeMillis(),
                    imgUrl = imageUrl
                )
                saveNotificationToDb(notificationEntity)
            }


        }

        // title 또는 message가 없으면 알림을 생성하지 않음
        if (title.isNullOrBlank() || message.isNullOrBlank()) {
            Log.w(TAG, "Title or message is null or blank. Skipping notification.")
            return
        }

        // 알림 생성 (포그라운드/백그라운드 구분 없이)
        // MainActivity의 onNewIntent 또는 onCreate에서 link 등을 확인하여 처리
        serviceScope.launch {
            val imageBitmap = imageUrl?.let { getBitmapFromUrl(it) }
            sendNotification(title, message, imageBitmap, link)
        }


    }


    private fun saveNotificationToDb(notification: NotificationEntity) {
        serviceScope.launch {
            try {
                notificationDao.insertNotification(notification)
                Log.d("FCM", "Notification saved to DB: $notification")
            } catch (e: Exception) {
                Log.e("FCM", "Error saving notification to DB", e)
            }
        }
    }

    private suspend fun getBitmapFromUrl(imageUrl: String): Bitmap? {
        if (imageUrl.isBlank()) return null
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(imageUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connectTimeout = 5000 // 5초 타임아웃
                connection.readTimeout = 5000    // 5초 타임아웃
                connection.connect()
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    connection.inputStream.buffered().use { input -> // .use 로 자동 close
                        BitmapFactory.decodeStream(input)
                    }
                } else {
                    Log.e(
                        TAG,
                        "Failed to download image from $imageUrl. Response code: ${connection.responseCode}"
                    )
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading image from $imageUrl: ${e.message}", e)
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
        // 알림 클릭 시 실행될 Intent 생성
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            link?.let {
                if (it.isNotBlank()) {
                    putExtra(EXTRA_LINK, it) // EXTRA_LINK 상수 사용
                    Log.d(TAG, "Notification link: '$it' added to intent extras.")
                }
            }

        }

        val notificationId =
            (System.currentTimeMillis() % Int.MAX_VALUE).toInt() // 또는 다른 고유 ID 생성 방식
        val requestCode = notificationId // PendingIntent의 requestCode도 고유하게

        // PendingIntent 플래그 설정
        val pendingIntentFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            pendingIntentFlag
        )

        // 알림 채널 ID (strings.xml 또는 상수로 정의하는 것이 좋음)
        val channelId = getString(R.string.default_notification_channel_id) // strings.xml 값 참조
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher) // 앱 아이콘으로 변경
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true) // 클릭 시 알림 자동 제거
            .setSound(defaultSoundUri)
            .setVibrate(longArrayOf(500, 500)) // 진동 패턴
            .setContentIntent(pendingIntent) // 알림 클릭 시 실행될 PendingIntent
            .setPriority(NotificationCompat.PRIORITY_HIGH) // 중요도 높게 설정

        // 이미지가 있는 경우 BigPictureStyle 적용
        imageBitmap?.let {
            notificationBuilder
                .setLargeIcon(it)
                .setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(it)
                        .bigLargeIcon(null as Bitmap?) // 확장 시에는 큰 아이콘 다시 표시 안 함
                )
        }

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Android Oreo (API 26) 이상에서는 알림 채널 필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = getString(R.string.default_notification_channel_name)
            val channelDescription = getString(R.string.default_notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH // 채널 중요도도 높게 설정

            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
                enableVibration(true)
                vibrationPattern = longArrayOf(500, 500)
                // 필요시 다른 채널 설정 (예: setAllowBubbles(true))
            }
            notificationManager.createNotificationChannel(channel)
        }

        // 알림 표시
        notificationManager.notify(notificationId, notificationBuilder.build())

    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        // 서버에 새 토큰을 등록하는 로직 (필요시 구현)
        // sendRegistrationToServer(token)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel() // 코루틴 작업 취소
    }
}