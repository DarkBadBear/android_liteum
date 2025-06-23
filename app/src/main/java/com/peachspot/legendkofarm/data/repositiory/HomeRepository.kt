package com.peachspot.legendkofarm.data.repositiory


import com.peachspot.legendkofarm.data.db.FarmLogDao
import com.peachspot.legendkofarm.data.db.FarmLogs
import kotlinx.coroutines.flow.Flow

interface HomeRepository {
    suspend fun insertLog(log: FarmLogs): Long
    suspend fun updateLog(log: FarmLogs): Int
    suspend fun deleteLog(log: FarmLogs): Int
    fun getLogById(id: Long): Flow<FarmLogs?>
    fun getLogsByDate(date: String): Flow<List<FarmLogs>>
    fun getAllLogs(): Flow<List<FarmLogs>>
    fun getAllLoggedDates(): Flow<List<String>>
    suspend fun deleteLogById(id: Long): Int
    suspend fun deleteAllLogs(): Int
    suspend fun insertAll(exercises: List<FarmLogs>)
}



class HomeRepositoryImpl(
    private val FarmLogDao: FarmLogDao
) : HomeRepository { // ': HomeRepository' 추가

    override suspend fun insertLog(log: FarmLogs): Long {
        return FarmLogDao.insert(log)
    }

    override suspend fun updateLog(log: FarmLogs): Int {
        return FarmLogDao.update(log)
    }

    override suspend fun deleteLog(log: FarmLogs): Int {
        return FarmLogDao.delete(log)
    }

    override fun getLogById(id: Long): Flow<FarmLogs?> {
        return FarmLogDao.getLogById(id)
    }

    override fun getLogsByDate(date: String): Flow<List<FarmLogs>> {
        return FarmLogDao.getLogsByDate(date)
    }

    override fun getAllLogs(): Flow<List<FarmLogs>> {
        return FarmLogDao.getAllLogs()
    }

    override fun getAllLoggedDates(): Flow<List<String>> {
        return FarmLogDao.getAllLoggedDates()
    }

    override suspend fun deleteLogById(id: Long): Int {
        return FarmLogDao.deleteLogById(id)
    }

    override suspend fun deleteAllLogs(): Int {
        return FarmLogDao.deleteAllLogs()
    }

    override suspend fun insertAll(exercises: List<FarmLogs>) {
        FarmLogDao.insertAll(exercises)
    }
}