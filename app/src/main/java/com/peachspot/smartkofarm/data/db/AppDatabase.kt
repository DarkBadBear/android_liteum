package com.peachspot.smartkofarm.data.db


import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

// Converters 클래스를 정확히 임포트해야 합니다.
 import com.peachspot.smartkofarm.util.Converters // Converters 클래스의 실제 위치에 맞게 수정

@TypeConverters(Converters::class) // TypeConverter 등록
@Database(
    entities = [
        FarmLogs::class,
        NotificationEntity::class
    ],
    version = 5, // 실제 데이터베이스 버전에 맞게 수정
    exportSchema = false
)
// AppDatabase 클래스를 public으로 변경합니다. (Kotlin에서 abstract 클래스는 기본적으로 open 입니다)
public abstract class AppDatabase : RoomDatabase() {

    abstract fun farmLogDao(): FarmLogDao
    abstract fun notificationDao(): NotificationDao // <<< 알림 DAO 추가

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private const val DATABASE_NAME = "smartkofarm_database"

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration() // 마이그레이션 전략에 따라 변경 가능
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
