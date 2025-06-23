package com.peachspot.legendkofarm.data.db


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    // 최신 알림 순으로 정렬하여 가져오기
    @Query("SELECT * FROM notifications ORDER BY receivedTimeMillis DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    // 특정 ID의 알림 삭제 (선택 사항)
    @Query("DELETE FROM notifications WHERE id = :notificationId")
    suspend fun deleteNotificationById(notificationId: Int)

    // 모든 알림 삭제 (선택 사항)
    @Query("DELETE FROM notifications")
    suspend fun clearAllNotifications()
}