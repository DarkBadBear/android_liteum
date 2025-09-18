package com.peachspot.liteum.data.db

import androidx.paging.PagingSource
import androidx.room.Dao

import androidx.room.Delete

import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.peachspot.liteum.data.model.BookWithReviews
import kotlinx.coroutines.flow.Flow

@Dao
interface BookLogsDao {

    /**
     * 새로운 책 정보를 삽입합니다. 충돌 발생 시 기존 데이터를 대체합니다.
     * @param bookLog 삽입할 BookLogs (책 정보) 객체
     * @return 삽입된 아이템의 row ID (Long). 삽입 실패 시 -1을 반환할 수 있습니다.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookLog(bookLog: BookLogs): Long

    /**
     * 여러 책 정보를 한 번에 삽입합니다. 충돌 발생 시 기존 데이터를 대체합니다.
     * @param bookLogs 삽입할 BookLogs (책 정보) 리스트
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllBookLogs(bookLogs: List<BookLogs>)

    /**
     * 기존 책 정보를 업데이트합니다.
     * @param bookLog 업데이트할 BookLogs (책 정보) 객체
     * @return 업데이트된 행의 수 (Int)
     */
    @Update
    suspend fun updateBookLog(bookLog: BookLogs): Int

    /**
     * 특정 책 정보를 삭제합니다.
     * @param bookLog 삭제할 BookLogs (책 정보) 객체
     * @return 삭제된 행의 수 (Int)
     */
    @Delete
    suspend fun deleteBookLog(bookLog: BookLogs): Int

    /**
     * 로컬 ID를 사용하여 특정 책 정보를 가져옵니다.
     * @param id 가져올 책 정보의 로컬 ID
     * @return 해당 ID의 BookLogs 객체 (Flow 형태, 없을 경우 null 포함 가능)
     */
    @Query("SELECT * FROM books WHERE id = :id")
    fun getBookLogById(id: Long): Flow<BookLogs?>



    /**
     * 특정 사용자가 등록한 모든 책 정보를 가져옵니다 (최신순 정렬).
     * 여기서 member_id는 이 책 정보를 '등록'한 사용자를 의미할 수 있습니다.
     * 또는 이 책 정보를 '소장'하고 있는 사용자를 의미할 수도 있습니다. (의미 명확화 필요)
     * @param memberId 조회할 사용자의 ID
     * @return 해당 사용자의 BookLogs 리스트 (Flow 형태)
     */
    @Query("SELECT * FROM books WHERE member_id = :memberId ORDER BY created_at_millis DESC")
    fun getBookLogsByMemberId(memberId: String): Flow<List<BookLogs>>

    /**
     * 모든 책 정보를 가져옵니다 (최신순 정렬).
     * @return 모든 BookLogs 리스트 (Flow 형태)
     */
    @Query("SELECT * FROM books ORDER BY created_at_millis DESC")
    fun getAllBookLogsFlow(): Flow<List<BookLogs>>

    // --- 페이징 기능을 위한 쿼리 ---

    /**
     * 모든 책 정보를 페이징하여 가져옵니다 (최신순 정렬).
     * @return PagingSource<Int, BookLogs>
     */
    @Query("SELECT * FROM books ORDER BY created_at_millis DESC")
    fun getAllBookLogsPaged(): PagingSource<Int, BookLogs>

    /**
     * 특정 사용자가 등록/소장한 모든 책 정보를 페이징하여 가져옵니다 (최신순 정렬).
     * @param memberId 조회할 사용자의 ID
     * @return PagingSource<Int, BookLogs>
     */
    @Query("SELECT * FROM books WHERE member_id = :memberId ORDER BY created_at_millis DESC")
    fun getBookLogsByMemberIdPaged(memberId: String): PagingSource<Int, BookLogs>

    /**
     * 책 제목 또는 저자 이름으로 책 정보를 검색하여 페이징합니다 (최신순 정렬).
     * @param query 검색어 (예: "%검색어%")
     * @return PagingSource<Int, BookLogs>
     */
    @Query("SELECT * FROM books WHERE book_title LIKE :query OR author LIKE :query ORDER BY created_at_millis DESC")
    fun searchBookLogsPaged(query: String): PagingSource<Int, BookLogs>


    /**
     * 특정 로컬 ID의 책 정보를 삭제합니다.
     * @param id 삭제할 책 정보의 로컬 ID
     * @return 삭제된 행의 수 (Int).
     */
    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteBookLogById(id: Long): Int

    /**
     * 특정 서버 ID의 책 정보를 삭제합니다.
     * @param serverId 삭제할 책 정보의 서버 ID
     * @return 삭제된 행의 수 (Int).
     */

    /**
     * 특정 사용자가 등록/소장한 모든 책 정보를 삭제합니다.
     * @param memberId 삭제할 책 정보들의 사용자 ID
     * @return 삭제된 총 행의 수 (Int)
     */
    @Query("DELETE FROM books WHERE member_id = :memberId")
    suspend fun deleteAllBookLogsByMemberId(memberId: String): Int

    /**
     * 모든 책 정보를 삭제합니다.
     * **경고:** 이 작업은 모든 책 정보 기록을 영구적으로 삭제합니다.
     * @return 삭제된 총 행의 수 (Int)
     */
    @Query("DELETE FROM books")
    suspend fun deleteAllBookLogs(): Int


    @Transaction // 이 쿼리는 원자적으로 실행됩니다.
    @Query("SELECT * FROM books ORDER BY created_at_millis DESC") // BookLogs 테이블을 기준으로 조회
    fun getAllBooksWithReviewsPaged(): PagingSource<Int, BookWithReviews>

    /**
     * 특정 사용자가 등록한 모든 책 정보를 페이징하여 가져오되, 각 책에 연결된 모든 리뷰 정보도 함께 가져옵니다.
     * @param memberId 조회할 사용자의 ID (BookLogs의 member_id)
     * @return PagingSource<Int, BookWithReviews>
     */
    @Transaction
    @Query("SELECT * FROM books WHERE member_id = :memberId ORDER BY created_at_millis DESC")
    fun getBooksWithReviewsByMemberIdPaged(memberId: String): PagingSource<Int, BookWithReviews>
}

