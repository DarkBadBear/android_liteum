package com.peachspot.legendkofarm.data.db


import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow // Flow를 사용하여 비동기 데이터 스트림 처리

@Dao
interface FarmLogDao {

    /**
     * 새로운 운동 기록을 삽입합니다. 충돌 발생 시 기존 데이터를 대체합니다.
     * @param log 삽입할 FarmLog 객체
     * @return 삽입된 아이템의 row ID (Long). 삽입 실패 시 -1을 반환할 수 있습니다.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: FarmLogs): Long

    /**
     * 기존 운동 기록을 업데이트합니다.
     * @param log 업데이트할 FarmLog 객체
     * @return 업데이트된 행의 수 (Int)
     */
    @Update
    suspend fun update(log: FarmLogs): Int

    /**
     * 특정 운동 기록을 삭제합니다.
     * @param log 삭제할 FarmLog 객체
     * @return 삭제된 행의 수 (Int)
     */
    @Delete
    suspend fun delete(log: FarmLogs): Int

    /**
     * ID를 사용하여 특정 운동 기록을 가져옵니다.
     * @param id 가져올 운동 기록의 ID
     * @return 해당 ID의 FarmLog 객체 (Flow 형태, 없을 경우 null 포함 가능)
     */
    @Query("SELECT * FROM Farm_logs WHERE id = :id")
    fun getLogById(id: Long): Flow<FarmLogs?>

    /**
     * 특정 날짜의 모든 운동 기록을 시작 시간(내림차순)으로 정렬하여 가져옵니다.
     * @param date 조회할 날짜 (예: "YYYY-MM-DD")
     * @return 해당 날짜의 FarmLog 리스트 (Flow 형태)
     */
    @Query("SELECT * FROM Farm_logs WHERE date = :date ORDER BY start_time_millis DESC")
    fun getLogsByDate(date: String): Flow<List<FarmLogs>>

    /**
     * 모든 운동 기록을 날짜(내림차순), 그 다음 시작 시간(내림차순)으로 정렬하여 가져옵니다.
     * @return 모든 FarmLog 리스트 (Flow 형태)
     */
    @Query("SELECT * FROM Farm_logs ORDER BY date DESC, start_time_millis DESC")
    fun getAllLogs(): Flow<List<FarmLogs>>

    /**
     * 운동 기록이 있는 모든 날짜를 중복 없이 가져옵니다 (최신 날짜 순).
     * @return 날짜 문자열 리스트 (Flow 형태, "YYYY-MM-DD" 형식)
     */
    @Query("SELECT DISTINCT date FROM Farm_logs ORDER BY date DESC")
    fun getAllLoggedDates(): Flow<List<String>>

    /**
     * 특정 ID의 운동 기록을 삭제합니다.
     * @param id 삭제할 운동 기록의 ID
     * @return 삭제된 행의 수 (Int). 일반적으로 0 또는 1이 됩니다.
     */
    @Query("DELETE FROM Farm_logs WHERE id = :id")
    suspend fun deleteLogById(id: Long): Int

    /**
     * 모든 운동 기록을 삭제합니다.
     * **경고:** 이 작업은 되돌릴 수 없으며 모든 기록이 영구적으로 삭제됩니다. 신중하게 사용하세요.
     * @return 삭제된 총 행의 수 (Int)
     */
    @Query("DELETE FROM Farm_logs")
    suspend fun deleteAllLogs(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE) // Or your preferred conflict strategy
    suspend fun insertAll(Farms: List<FarmLogs>)

}