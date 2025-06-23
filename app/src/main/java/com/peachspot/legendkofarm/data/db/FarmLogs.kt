package com.peachspot.legendkofarm.data.db


import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.peachspot.legendkofarm.util.TimestampListConverter

@Entity(tableName = "farm_logs")
@TypeConverters(TimestampListConverter::class)
data class FarmLogs(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "date")
    val date: String,          // 운동 날짜 (예: "YYYY-MM-DD" 형식의 문자열)

    @ColumnInfo(name = "start_time_millis")
    val startTimeMillis: Long,     // 운동 시작 시간 (epoch milliseconds)

    @ColumnInfo(name = "end_time_millis")
    val endTimeMillis: Long,       // 운동 종료 시간 (epoch milliseconds)

    @ColumnInfo(name = "duration_seconds")
    val durationSeconds: Long, // 총 운동 시간 (초 단위)

    @ColumnInfo(name = "total_floors")
    val totalFloors: Int,

    @ColumnInfo(name = "calories_burned")
    val caloriesBurned: Double,

    @ColumnInfo(name = "floor_height_meters")
    val floorHeight: Float,

    @ColumnInfo(name = "floor_count_per_lap")
    val floorCountPerLap: Int,

    @ColumnInfo(name = "lap_count")
    val laps: Int,

    @ColumnInfo(name = "arrival_timestamps_millis")
    val arrivalTimestampsMillis: List<Long> // 각 랩별 도착 시간 기록 (epoch milliseconds 리스트)
)
