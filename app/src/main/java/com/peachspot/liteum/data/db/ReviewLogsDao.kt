package com.peachspot.liteum.data.db // 또는 DAO를 모아두는 다른 패키지

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow@Dao
interface ReviewLogsDao {

    /**
     * 새로운 사용자 리뷰를 삽입합니다. 충돌 발생 시 기존 데이터를 대체합니다.
     * @param reviewLog 삽입할 ReviewLogs 객체
     * @return 삽입된 아이템의 row ID (Long).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reviewLog: ReviewLogs): Long

    /**
     * 여러 사용자 리뷰를 한 번에 삽입합니다. 충돌 발생 시 기존 데이터를 대체합니다.
     * @param reviewLogs 삽입할 ReviewLogs 리스트
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reviewLogs: List<ReviewLogs>)

    /**
     * 기존 사용자 리뷰를 업데이트합니다.
     * @param reviewLog 업데이트할 ReviewLogs 객체
     * @return 업데이트된 행의 수 (Int)
     */
    @Update
    suspend fun update(reviewLog: ReviewLogs): Int

    /**
     * 특정 사용자 리뷰를 삭제합니다.
     * @param reviewLog 삭제할 ReviewLogs 객체
     * @return 삭제된 행의 수 (Int)
     */
    @Delete
    suspend fun delete(reviewLog: ReviewLogs): Int

    /**
     * 로컬 ID를 사용하여 특정 사용자 리뷰를 가져옵니다.
     * @param id 가져올 리뷰의 로컬 ID
     * @return 해당 ID의 ReviewLogs 객체 (Flow 형태, 없을 경우 null 포함 가능)
     */
    @Query("SELECT * FROM book_reviews WHERE id = :id")
    fun getReviewByLocalId(id: Long): Flow<ReviewLogs?>


    /**
     * 특정 책(bookLogLocalId 기준)에 달린 모든 리뷰를 가져옵니다 (최신순 정렬).
     * @param bookLogLocalId 조회할 책의 로컬 ID
     * @return 해당 책의 ReviewLogs 리스트 (Flow 형태)
     */
    @Query("SELECT * FROM book_reviews WHERE book_log_local_id = :bookLogLocalId ORDER BY created_at_millis DESC")
    fun getReviewsForBook(bookLogLocalId: Long): Flow<List<ReviewLogs>>

    /**
     * 특정 책(bookLogLocalId 기준)에 달린 모든 리뷰를 페이징하여 가져옵니다 (최신순 정렬).
     * @param bookLogLocalId 조회할 책의 로컬 ID
     * @return PagingSource<Int, ReviewLogs>
     */
    @Query("SELECT * FROM book_reviews WHERE book_log_local_id = :bookLogLocalId ORDER BY created_at_millis DESC")
    fun getReviewsForBookPaged(bookLogLocalId: Long): PagingSource<Int, ReviewLogs>


    /**
     * 특정 사용자가 작성한 모든 리뷰를 가져옵니다 (최신순 정렬).
     * @param memberId 조회할 사용자의 ID
     * @return 해당 사용자의 ReviewLogs 리스트 (Flow 형태)
     */
    @Query("SELECT * FROM book_reviews WHERE member_id = :memberId ORDER BY created_at_millis DESC")
    fun getReviewsByMember(memberId: String): Flow<List<ReviewLogs>>

    /**
     * 특정 사용자가 작성한 모든 리뷰를 페이징하여 가져옵니다 (최신순 정렬).
     * @param memberId 조회할 사용자의 ID
     * @return PagingSource<Int, ReviewLogs>
     */
    @Query("SELECT * FROM book_reviews WHERE member_id = :memberId ORDER BY created_at_millis DESC")
    fun getReviewsByMemberPaged(memberId: String): PagingSource<Int, ReviewLogs>

    /**
     * 로컬 ID로 특정 리뷰를 삭제합니다.
     * @param id 삭제할 리뷰의 로컬 ID
     */
    @Query("DELETE FROM book_reviews WHERE id = :id")
    suspend fun deleteReviewByLocalId(id: Long): Int



    /**
     * 특정 책(bookLogLocalId 기준)에 달린 모든 리뷰를 삭제합니다.
     * @param bookLogLocalId 리뷰를 삭제할 책의 로컬 ID
     */
    @Query("DELETE FROM book_reviews WHERE book_log_local_id = :bookLogLocalId")
    suspend fun deleteAllReviewsForBook(bookLogLocalId: Long): Int

    /**
     * 특정 사용자가 작성한 모든 리뷰를 삭제합니다.
     * @param memberId 리뷰를 삭제할 사용자 ID
     */
    @Query("DELETE FROM book_reviews WHERE member_id = :memberId")
    suspend fun deleteAllReviewsByMember(memberId: String): Int

    /**
     * 모든 리뷰를 삭제합니다. (주의해서 사용)
     */
    @Query("DELETE FROM book_reviews")
    suspend fun deleteAllReviews(): Int
}
