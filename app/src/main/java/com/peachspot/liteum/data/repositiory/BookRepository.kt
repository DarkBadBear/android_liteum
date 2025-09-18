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
    suspend fun insertReviewLog(reviewLog: ReviewLogs): Long // 반환 타입은 ReviewLogsDao의 insert와 일치하도록 Long
}


class BookRepositoryImpl(
    private val bookLogsDao: BookLogsDao,
    private val reviewLogsDao: ReviewLogsDao // 생성자에 ReviewLogsDao 주입 추가

) : BookRepository {
    // 추가된 insertReviewLog 메서드 구현
    override suspend fun insertReviewLog(reviewLog: ReviewLogs): Long {
        return reviewLogsDao.insert(reviewLog) // ReviewLogsDao의 insert 메서드 호출
    }
    companion object {
        private const val DEFAULT_PAGE_SIZE = 20
    }

    // BookWithReviews (DB 데이터)를 FeedItem (UI 모델)으로 변환하는 함수
    private fun mapBookWithReviewsToFeedItem(bookWithReviews: BookWithReviews): FeedItem {
        val bookLog = bookWithReviews.book
        val reviewLogsList = bookWithReviews.reviews

        val feedItemId = bookLog.id.toString()

        val uiBookReviews = reviewLogsList.map { reviewLogDb ->
            BookReview( // 여기서 사용하는 BookReview는 com.peachspot.liteum.data.model.BookReview
                id = reviewLogDb.id.toString(),
                // userId, reviewerName, reviewText, rating, content, timestamp 등은
                // 이전 답변의 BookReview UI 모델 정의를 따른다고 가정합니다.
                // 만약 BookReview 모델이 다르다면 이 부분도 맞춰야 합니다.
                // 예시 (이전 BookReview 모델 정의 기준):
                userId = reviewLogDb.memberId,
                reviewerName = reviewLogDb.memberId,
                reviewText = reviewLogDb.reviewText,
                rating = 0.0f, // ReviewLogs에 rating이 없으므로 기본값 또는 모델 수정 필요
                content = reviewLogDb.reviewText,
                timestamp = reviewLogDb.createdAtMillis
            )
        }

        return FeedItem( // 여기서 사용하는 FeedItem은 com.peachspot.liteum.data.model.FeedItem
            id = feedItemId,
            userName = bookLog.member_id ?: "익명",
            userProfileImageUrl = "https://picsum.photos/seed/user${bookLog.member_id?.hashCode() ?: 0}/100/100",
            bookImageUrl = bookLog.coverImageUri,
            bookTitle = bookLog.bookTitle,
            caption = "저자: ${bookLog.author ?: "정보 없음"}",
            likes = (bookLog.id % 50).toInt() + 10,
            // commentsCount 필드가 FeedItem에서 제거되었으므로 여기도 제거
            timestamp = bookLog.createdAtMillis,
            reviews = uiBookReviews
        )
    }

    // BookLogs (리뷰 정보 없는 책 데이터)를 FeedItem (리뷰 없는 UI 모델)으로 변환하는 함수
    private fun mapBookLogOnlyToFeedItem(bookLog: BookLogs): FeedItem {
        return FeedItem(
            id = bookLog.id.toString(),
            userName = bookLog.member_id ?: "익명",
            userProfileImageUrl = "https://picsum.photos/seed/user${bookLog.member_id?.hashCode() ?: 0}/100/100",
            bookImageUrl = bookLog.coverImageUri,
            bookTitle = bookLog.bookTitle,
            caption = "저자: ${bookLog.author ?: "정보 없음"}. (평점: ${bookLog.rating ?: "미등록"})",
            likes = (bookLog.id % 50).toInt() + 10,
            // commentsCount 필드가 FeedItem에서 제거되었으므로 여기도 제거
            timestamp = bookLog.createdAtMillis,
            reviews = emptyList()
        )
    }

    override fun getBookFeedItemsPaged(): Flow<PagingData<FeedItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = DEFAULT_PAGE_SIZE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { bookLogsDao.getAllBooksWithReviewsPaged() }
        ).flow.map { pagingData: PagingData<BookWithReviews> ->
            pagingData.map { bookWithReviews ->
                mapBookWithReviewsToFeedItem(bookWithReviews)
            }
        }
    }

    override fun getBookFeedItemsByMemberIdPaged(memberId: String): Flow<PagingData<FeedItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = DEFAULT_PAGE_SIZE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { bookLogsDao.getBooksWithReviewsByMemberIdPaged(memberId) }
        ).flow.map { pagingData: PagingData<BookWithReviews> ->
            pagingData.map { bookWithReviews ->
                mapBookWithReviewsToFeedItem(bookWithReviews)
            }
        }
    }

    override fun searchBookFeedItemsPaged(query: String): Flow<PagingData<FeedItem>> {
        val searchQuery = if (query.isBlank()) "" else "%${query.trim()}%"
        return Pager(
            config = PagingConfig(
                pageSize = DEFAULT_PAGE_SIZE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                if (searchQuery.isNotEmpty()) {
                    bookLogsDao.searchBookLogsPaged(searchQuery)
                } else {
                    bookLogsDao.getAllBookLogsPaged()
                }
            }
        ).flow.map { pagingData: PagingData<BookLogs> ->
            pagingData.map { bookLog ->
                mapBookLogOnlyToFeedItem(bookLog)
            }
        }
    }

    override fun getAllBookFeedItemsFlow(): Flow<List<FeedItem>> {
        return bookLogsDao.getAllBookLogsFlow()
            .map { logsList ->
                logsList.map { bookLog -> mapBookLogOnlyToFeedItem(bookLog) }
            }
    }

    override suspend fun getAllBookFeedItemsList(): List<FeedItem> {
        val allLogsList = bookLogsDao.getAllBookLogsFlow().first()
        return allLogsList.map { bookLog -> mapBookLogOnlyToFeedItem(bookLog) }
    }

    // === 책(BookLogs) CRUD 관련 메서드 구현 ===
    override fun getBookLogById(id: Long): Flow<BookLogs?> {
        return bookLogsDao.getBookLogById(id)
    }

    override suspend fun updateBookLog(bookLog: BookLogs): Int {
        return bookLogsDao.updateBookLog(bookLog)
    }

    override suspend fun insertBookLog(bookLog: BookLogs): Long {
        return bookLogsDao.insertBookLog(bookLog)
    }

    override suspend fun deleteBookLogById(id: Long): Int {
        return bookLogsDao.deleteBookLogById(id)
    }

    override fun getBookLogsByMemberId(memberId: String): Flow<List<BookLogs>> {
        return bookLogsDao.getBookLogsByMemberId(memberId)
    }
}
