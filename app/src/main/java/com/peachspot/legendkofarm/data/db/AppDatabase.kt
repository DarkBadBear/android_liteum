package com.peachspot.legendkofarm.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

// Converters 클래스의 실제 위치에 맞게 수정
import com.peachspot.legendkofarm.util.Converters

@TypeConverters(Converters::class) // TypeConverter 등록
@Database(
    entities = [
        UrlEntity::class,
        NotificationEntity::class // NotificationEntity 클래스가 정의되어 있어야 합니다.
    ],
    version = 6, // 데이터베이스 버전. 스키마 변경 시 버전을 올려야 합니다.
    exportSchema = false // 스키마 파일을 내보내지 않음. (프로덕션에서는 true 권장)
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun urlDao(): UrlDao
    abstract fun notificationDao(): NotificationDao // 알림 DAO 추가 (NotificationDao가 정의되어 있어야 합니다)

    companion object {
        @Volatile // 다른 스레드에 즉시 가시성을 보장합니다.
        private var INSTANCE: AppDatabase? = null
        private const val DATABASE_NAME = "legendkofarm_database"

        // getInstance로 이름 변경하여 UrlCheckWorker에서 호출하는 이름과 일치시킴
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    // 데이터베이스 스키마 변경 시 기존 데이터를 파괴하고 다시 생성합니다.
                    // 실제 앱에서는 마이그레이션 전략을 신중하게 고려해야 합니다.
                    .fallbackToDestructiveMigration(false) // 마이그레이션 전략에 따라 변경 가능
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
