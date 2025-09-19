package com.peachspot.liteum.data.repositiory

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.peachspot.liteum.data.db.BookLogsDao
import com.peachspot.liteum.data.db.BookLogs // BookLogs 엔티티
import com.peachspot.liteum.data.db.ReviewLogs // ReviewLogs 엔티티 (매핑 시 필요)
import com.peachspot.liteum.data.db.ReviewLogsDao
import com.peachspot.liteum.data.model.FeedItem
import com.peachspot.liteum.data.model.BookReview
import com.peachspot.liteum.data.model.BookWithReviews
import com.peachspot.liteum.data.remote.api.MyApiService
import com.peachspot.liteum.data.remote.model.BookResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

interface BookRepository {
    fun getBookFeedItemsPaged(): Flow<PagingData<FeedItem>>
    fun getBookFeedItemsByMemberIdPaged(memberId: String): Flow<PagingData<FeedItem>>
    fun searchBookFeedItemsPaged(query: String): Flow<PagingData<FeedItem>>
    fun getAllBookFeedItemsFlow(): Flow<List<FeedItem>>
    suspend fun getAllBookFeedItemsList(): List<FeedItem>
    fun getBookLogById(id: Long): Flow<BookLogs?>
    suspend fun updateBookLog(bookLog: BookLogs): Int
    suspend fun insertBookLog(bookLog: BookLogs): Long
    suspend fun deleteBookLogById(id: Long): Int
    fun getBookLogsByMemberId(memberId: String): Flow<List<BookLogs>>
    suspend fun insertReviewLog(reviewLog: ReviewLogs): Long
    // searchBooks 함수 시그니처는 MyApiService의 searchBooksOnGoogle과 일치하도록 조정
    // 또는 MyApiService의 함수 시그니처를 BookRepository 인터페이스에 맞게 조정
    suspend fun searchBooks(query: String, firebaseUid: String?, kakaoUid: String?): BookResponse
}

class BookRepositoryImpl(
    private val bookLogsDao: BookLogsDao,
    private val reviewLogsDao: ReviewLogsDao,
    private val myApiService: MyApiService // MyApiService 주입은 잘 되어 있음
) : BookRepository {

    // searchBooks 함수 구현 추가
    override suspend fun searchBooks(query: String, firebaseUid: String?, kakaoUid: String?): BookResponse {
        // MyApiService의 해당 함수를 호출합니다.
        // MyApiService.searchBooksOnGoogle의 query 파라미터 이름이 "query"인지 확인하세요.
        // 이전 MyApiService 정의에서는 @Query("query") query: String? 로 되어 있었습니다.
        return myApiService.searchBooksOnGoogle( // MyApiService에 정의된 함수 이름 사용
            firebaseUid = firebaseUid,
            kakaoUid = kakaoUid,
            query = query
        )
    }

    override suspend fun insertReviewLog(reviewLog: ReviewLogs): Long {
        return reviewLogsDao.insert(reviewLog)
    }

    companion object {
        private const val DEFAULT_PAGE_SIZE = 20
    }

    private fun mapBookWithReviewsToFeedItem(bookWithReviews: BookWithReviews): FeedItem {
        val bookLog = bookWithReviews.book
        val reviewLogsList = bookWithReviews.reviews
        val feedItemId = bookLog.id.toString()
        val uiBookReviews = reviewLogsList.map { reviewLogDb ->
            BookReview(
                id = reviewLogDb.id.toString(),
                userId = reviewLogDb.memberId,
                reviewerName = reviewLogDb.memberId, // 실제 사용자 이름 필드가 있다면 그것을 사용
                reviewText = reviewLogDb.reviewText,
                rating = 0.0f, // ReviewLogs에 rating이 없다면 기본값 또는 모델 수정
                content = reviewLogDb.reviewText, // reviewText와 content가 같다면 하나로 통일 고려
                timestamp = reviewLogDb.createdAtMillis
            )
        }
        return FeedItem(
            id = feedItemId,
            userName = bookLog.member_id ?: "익명",
            userProfileImageUrl = "https://picsum.photos/seed/user${bookLog.member_id?.hashCode() ?: 0}/100/100",
            bookImageUrl = bookLog.coverImageUri,
            bookTitle = bookLog.bookTitle,
            caption = "저자: ${bookLog.author ?: "정보 없음"}",
            likes = (bookLog.id % 50).toInt() + 10,
            timestamp = bookLog.createdAtMillis,
            reviews = uiBookReviews,
            isbn = bookLog.isbn
        )
    }

    private fun mapBookLogOnlyToFeedItem(bookLog: BookLogs): FeedItem {
        return FeedItem(
            id = bookLog.id.toString(),
            userName = bookLog.member_id ?: "익명",
            userProfileImageUrl = "https://picsum.photos/seed/user${bookLog.member_id?.hashCode() ?: 0}/100/100",
            bookImageUrl = bookLog.coverImageUri,
            bookTitle = bookLog.bookTitle,
            caption = "저자: ${bookLog.author ?: "정보 없음"}. (평점: ${bookLog.rating ?: "미등록"})",
            likes = (bookLog.id % 50).toInt() + 10,
            timestamp = bookLog.createdAtMillis,
            reviews = emptyList(),
            isbn = bookLog.isbn
        )
    }

    override fun getBookFeedItemsPaged(): Flow<PagingData<FeedItem>> {
        return Pager(
            config = PagingConfig(pageSize = DEFAULT_PAGE_SIZE, enablePlaceholders = false),
            pagingSourceFactory = { bookLogsDao.getAllBooksWithReviewsPaged() }
        ).flow.map { pagingData ->
            pagingData.map { mapBookWithReviewsToFeedItem(it) }
        }
    }

    override fun getBookFeedItemsByMemberIdPaged(memberId: String): Flow<PagingData<FeedItem>> {
        return Pager(
            config = PagingConfig(pageSize = DEFAULT_PAGE_SIZE, enablePlaceholders = false),
            pagingSourceFactory = { bookLogsDao.getBooksWithReviewsByMemberIdPaged(memberId) }
        ).flow.map { pagingData ->
            pagingData.map { mapBookWithReviewsToFeedItem(it) }
        }
    }

    override fun searchBookFeedItemsPaged(query: String): Flow<PagingData<FeedItem>> {
        val searchQuery = if (query.isBlank()) "" else "%${query.trim()}%"
        return Pager(
            config = PagingConfig(pageSize = DEFAULT_PAGE_SIZE, enablePlaceholders = false),
            pagingSourceFactory = {
                if (searchQuery.isNotEmpty()) {
                    bookLogsDao.searchBookLogsPaged(searchQuery)
                } else {
                    bookLogsDao.getAllBookLogsPaged()
                }
            }
        ).flow.map { pagingData ->
            pagingData.map { mapBookLogOnlyToFeedItem(it) }
        }
    }

    override fun getAllBookFeedItemsFlow(): Flow<List<FeedItem>> {
        return bookLogsDao.getAllBookLogsFlow().map { logsList ->
            logsList.map { mapBookLogOnlyToFeedItem(it) }
        }
    }

    override suspend fun getAllBookFeedItemsList(): List<FeedItem> {
        return bookLogsDao.getAllBookLogsFlow().first().map { mapBookLogOnlyToFeedItem(it) }
    }

    override fun getBookLogById(id: Long): Flow<BookLogs?> = bookLogsDao.getBookLogById(id)

    override suspend fun updateBookLog(bookLog: BookLogs): Int = bookLogsDao.updateBookLog(bookLog)

    override suspend fun insertBookLog(bookLog: BookLogs): Long = bookLogsDao.insertBookLog(bookLog)

    override suspend fun deleteBookLogById(id: Long): Int = bookLogsDao.deleteBookLogById(id)

    override fun getBookLogsByMemberId(memberId: String): Flow<List<BookLogs>> = bookLogsDao.getBookLogsByMemberId(memberId)
}
