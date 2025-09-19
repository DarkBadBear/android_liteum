package com.peachspot.liteum.data.repositiory // 실제 프로젝트의 패키지 경로로 수정하세요.

import android.util.Log
import androidx.paging.PagingData
import com.peachspot.liteum.data.db.ReviewLogs // ReviewLogs 엔티티 임포트
import kotlinx.coroutines.flow.Flow

import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.peachspot.liteum.data.db.ReviewLogsDao // ReviewLogsDao 임포트



/**
 * 사용자 리뷰 데이터 처리를 위한 Repository 인터페이스
 */
interface ReviewRepository {

    /**
     * 새로운 리뷰를 추가합니다.
     * @param reviewLog 추가할 ReviewLogs 객체
     * @return 삽입된 리뷰의 로컬 ID
     */
    suspend fun addReview(reviewLog: ReviewLogs): Long

    /**
     * 기존 리뷰를 업데이트합니다.
     * @param reviewLog 업데이트할 ReviewLogs 객체
     * @return 업데이트된 행의 수
     */
    suspend fun updateReview(reviewLog: ReviewLogs): Int

    /**
     * 특정 리뷰를 삭제합니다.
     * @param reviewLog 삭제할 ReviewLogs 객체
     * @return 삭제된 행의 수
     */
    suspend fun deleteReview(reviewLog: ReviewLogs): Int

    /**
     * 로컬 ID로 특정 리뷰를 가져옵니다.
     * @param reviewId 가져올 리뷰의 로컬 ID
     * @return 해당 ID의 ReviewLogs 객체 (Flow 형태, 없을 경우 null 포함 가능)
     */
    fun getReviewByLocalId(reviewId: Long): Flow<ReviewLogs?>

    /**
     * 특정 책(로컬 ID 기준)에 대한 모든 리뷰를 가져옵니다.
     * @param bookLogLocalId 리뷰를 가져올 책의 로컬 ID
     * @return 해당 책의 리뷰 리스트 (Flow 형태, 최신순 정렬)
     */
    fun getReviewsForBook(bookLogLocalId: Long): Flow<List<ReviewLogs>>

    /**
     * 특정 책(로컬 ID 기준)에 대한 모든 리뷰를 페이징하여 가져옵니다.
     * @param bookLogLocalId 리뷰를 가져올 책의 로컬 ID
     * @return 해당 책의 리뷰 PagingData (Flow 형태, 최신순 정렬)
     */
    fun getReviewsForBookPaged(bookLogLocalId: Long): Flow<PagingData<ReviewLogs>>

    /**
     * 특정 사용자가 작성한 모든 리뷰를 가져옵니다.
     * @param memberId 리뷰를 가져올 사용자의 ID
     * @return 해당 사용자의 리뷰 리스트 (Flow 형태, 최신순 정렬)
     */
    fun getReviewsByMember(memberId: String): Flow<List<ReviewLogs>>

    /**
     * 특정 사용자가 작성한 모든 리뷰를 페이징하여 가져옵니다.
     * @param memberId 리뷰를 가져올 사용자의 ID
     * @return 해당 사용자의 리뷰 PagingData (Flow 형태, 최신순 정렬)
     */
    fun getReviewsByMemberPaged(memberId: String): Flow<PagingData<ReviewLogs>>

    /**
     * 로컬 ID로 특정 리뷰를 삭제합니다.
     * @param reviewId 삭제할 리뷰의 로컬 ID
     * @return 삭제된 행의 수
     */
    suspend fun deleteReviewById(feedItemId: String, reviewId: String): Boolean // 성공 여부 반환 (예시)

    /**
     * 특정 책(로컬 ID 기준)에 대한 모든 리뷰를 삭제합니다.
     * @param bookLogLocalId 리뷰를 삭제할 책의 로컬 ID
     * @return 삭제된 총 행의 수
     */
    suspend fun deleteAllReviewsForBook(bookLogLocalId: Long): Int

    /**
     * 특정 사용자가 작성한 모든 리뷰를 삭제합니다.
     * @param memberId 리뷰를 삭제할 사용자 ID
     * @return 삭제된 총 행의 수
     */
    suspend fun deleteAllReviewsByMember(memberId: String): Int

    // 필요하다면, 서버와 동기화 로직을 위한 함수들 추가
    // suspend fun syncReviewUpstream(reviewLog: ReviewLogs): Result<ReviewLogs> // 로컬 리뷰 서버에 업로드
    // suspend fun syncReviewsDownstream(bookLogLocalId: Long) // 특정 책의 리뷰를 서버에서 받아오기
}



/**
 * ReviewRepository의 구현체
 */
class ReviewRepositoryImpl(
    private val reviewLogsDao: ReviewLogsDao
    // private val reviewApiService: ReviewApiService? = null // 원격 API 서비스가 있다면 주입
) : ReviewRepository {

    companion object {
        private const val DEFAULT_PAGE_SIZE = 20 // 페이징 시 페이지 당 아이템 수
    }

    override suspend fun addReview(reviewLog: ReviewLogs): Long {
        // 추가적인 비즈니스 로직이 필요하다면 여기에 구현
        // 예를 들어, 타임스탬프 업데이트 등
        val currentTime = System.currentTimeMillis()
        val reviewToInsert = reviewLog.copy(
            createdAtMillis = if (reviewLog.createdAtMillis == 0L) currentTime else reviewLog.createdAtMillis,
            updatedAtMillis = currentTime // 추가 또는 수정 시 항상 현재 시간으로 업데이트
        )
        return reviewLogsDao.insert(reviewToInsert)
    }

    override suspend fun updateReview(reviewLog: ReviewLogs): Int {
        val currentTime = System.currentTimeMillis()
        val reviewToUpdate = reviewLog.copy(
            updatedAtMillis = currentTime // 수정 시 업데이트 시간 갱신
        )
        return reviewLogsDao.update(reviewToUpdate)
    }

    override suspend fun deleteReview(reviewLog: ReviewLogs): Int {
        return reviewLogsDao.delete(reviewLog)
    }

    override fun getReviewByLocalId(reviewId: Long): Flow<ReviewLogs?> {
        return reviewLogsDao.getReviewByLocalId(reviewId)
    }



    override fun getReviewsForBook(bookLogLocalId: Long): Flow<List<ReviewLogs>> {
        return reviewLogsDao.getReviewsForBook(bookLogLocalId)
    }

    override fun getReviewsForBookPaged(bookLogLocalId: Long): Flow<PagingData<ReviewLogs>> {
        return Pager(
            config = PagingConfig(
                pageSize = DEFAULT_PAGE_SIZE,
                enablePlaceholders = false // 일반적으로 false로 설정
            ),
            pagingSourceFactory = { reviewLogsDao.getReviewsForBookPaged(bookLogLocalId) }
        ).flow
    }

    override fun getReviewsByMember(memberId: String): Flow<List<ReviewLogs>> {
        return reviewLogsDao.getReviewsByMember(memberId)
    }

    override fun getReviewsByMemberPaged(memberId: String): Flow<PagingData<ReviewLogs>> {
        return Pager(
            config = PagingConfig(
                pageSize = DEFAULT_PAGE_SIZE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { reviewLogsDao.getReviewsByMemberPaged(memberId) }
        ).flow
    }


    override suspend fun deleteReviewById(feedItemId: String, reviewId: String): Boolean {
        return try {
            // 로컬 DB에서 삭제 (예시)
            val deletedRows = reviewLogsDao.deleteReviewById(reviewId) // DAO에 해당 함수 구현 필요

            // API 서버에 삭제 요청 (예시)
            // val response = myApiService.deleteReview(feedItemId, reviewId)

            // 성공 조건 (예시: 로컬에서 1개 이상 삭제되었거나, API 응답이 성공인 경우)
            deletedRows > 0 // 또는 response.isSuccessful
        } catch (e: Exception) {
            // 오류 로깅 등
            Log.e("ReviewRepository", "Error deleting review", e)
            false
        }
    }

    override suspend fun deleteAllReviewsForBook(bookLogLocalId: Long): Int {
        return reviewLogsDao.deleteAllReviewsForBook(bookLogLocalId)
    }

    override suspend fun deleteAllReviewsByMember(memberId: String): Int {
        return reviewLogsDao.deleteAllReviewsByMember(memberId)
    }

    // 서버 동기화 로직 구현 예시 (주석 처리)
    /*
    override suspend fun syncReviewUpstream(reviewLog: ReviewLogs): Result<ReviewLogs> {
        if (reviewApiService == null) return Result.failure(Exception("ApiService not available"))
        return try {
            // 1. 서버에 리뷰 업로드 시도
            val response = if (reviewLog.serverReviewId == null || reviewLog.serverReviewId == 0L) {
                reviewApiService.postReview(reviewLog.toServerRequest()) // 새 리뷰 생성 API 호출
            } else {
                reviewApiService.updateReview(reviewLog.serverReviewId, reviewLog.toServerRequest()) // 기존 리뷰 업데이트 API 호출
            }

            if (response.isSuccessful && response.body() != null) {
                // 2. 서버 응답으로 받은 정보로 로컬 리뷰 업데이트 (예: 서버 ID, 업데이트된 타임스탬프)
                val updatedReviewFromServer = response.body()!!.toReviewLogsEntity(reviewLog.bookLogLocalId) // 서버 응답을 로컬 엔티티로 변환
                reviewLogsDao.insert(updatedReviewFromServer) // OnConflictStrategy.REPLACE 사용
                Result.success(updatedReviewFromServer)
            } else {
                Result.failure(Exception("Server error: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncReviewsDownstream(bookLogLocalId: Long) {
        if (reviewApiService == null) return
        try {
            val response = reviewApiService.getReviewsForBook(bookLogLocalId) // 서버에서 해당 책의 리뷰 가져오기
            if (response.isSuccessful && response.body() != null) {
                val serverReviews = response.body()!!.map { it.toReviewLogsEntity(bookLogLocalId) }
                reviewLogsDao.insertAll(serverReviews) // 가져온 리뷰들을 로컬 DB에 저장 (충돌 시 대체)
            }
        } catch (e: Exception) {
            // 오류 처리
        }
    }

    // 서버 요청/응답 DTO와 엔티티 간 변환 함수 (예시)
    private fun ReviewLogs.toServerRequest(): ReviewServerRequest {
        return ReviewServerRequest(...) // 엔티티를 서버 요청 DTO로 변환
    }
    private fun ReviewServerResponse.toReviewLogsEntity(bookLogLocalId: Long): ReviewLogs {
        return ReviewLogs(...) // 서버 응답 DTO를 엔티티로 변환
    }
    */
}

