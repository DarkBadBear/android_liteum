package com.peachspot.legendkofarm.data.repositiory

import com.peachspot.legendkofarm.data.db.NotificationDao
import com.peachspot.legendkofarm.data.db.NotificationEntity

import kotlinx.coroutines.flow.Flow

class NotificationRepository(private val notificationDao: NotificationDao) {

    fun getAllNotifications(): Flow<List<NotificationEntity>> {
        return notificationDao.getAllNotifications()
    }

    // 필요하다면 다른 메서드 추가 (예: 알림 삭제)
    suspend fun clearAllNotifications() {
        notificationDao.clearAllNotifications()
    }
}