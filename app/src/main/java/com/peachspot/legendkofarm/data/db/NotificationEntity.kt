package com.peachspot.legendkofarm.data.db
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String?,
    val body: String?,
    val receivedTimeMillis: Long = System.currentTimeMillis(),
    var imgUrl: String?
)