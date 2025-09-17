package com.peachspot.liteum.data.repositiory // 패키지명은 실제 프로젝트에 맞게 확인해주세요

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.peachspot.liteum.data.db.BookLogs // 엔티티 BookLogs 사용
import com.peachspot.liteum.data.db.BookLogsDao // 책 정보를 가져오기 위한 DAO
import com.peachspot.liteum.ui.screens.FeedItem // UI 모델 FeedItem 사용 (또는 책 정보에 맞는 다른 UI 모델)
// import com.peachspot.liteum.ui.screens.BookReview // BookReview는 여기서 직접 사용하지 않음
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

interface HomeRepository {
    /**
     * 홈 화면에 표시할 모든 책 정보의 PagingData Flow를 가져옵니다.
     * 모든 책 정보를 페이징하여 FeedItem으로 변환합니다.
     */
    fun getBookFeedItemsPaged(): Flow<PagingData<FeedItem>>

    /**
     * 특정 사용자가 등록한 책 정보를 페이징하여 FeedItem의 PagingData Flow로 가져옵니다.
     * @param memberId 조회할 사용자의 ID
     */
    fun getBookFeedItemsByMemberIdPaged(memberId: String): Flow<PagingData<FeedItem>>

    /**
     * 책 제목 또는 저자 이름으로 검색하여 결과를 페이징하고 FeedItem의 PagingData Flow로 가져옵니다.
     * @param query 검색어
     */
    fun searchBookFeedItemsPaged(query: String): Flow<PagingData<FeedItem>>

    /**
     * 모든 책 정보를 FeedItem의 Flow로 변환하여 가져옵니다. (페이징 미적용)
     * 데이터 변경 시 UI 자동 업데이트에 유용하지만, 데이터가 많을 경우 성능 이슈 발생 가능.
     */
    fun getAllBookFeedItemsFlow(): Flow<List<FeedItem>>

    /**
     * (예시용) 모든 책 정보를 한 번에 List<FeedItem>으로 가져옵니다.
     * 데이터가 많을 경우 메모리 및 성능 문제를 야기할 수 있습니다.
     */
    suspend fun getAllBookFeedItemsList(): List<FeedItem>
}

class HomeRepositoryImpl(
    private val bookLogsDao: BookLogsDao // BookLogsDao 주입
) : HomeRepository {

    companion object {
        private const val DEFAULT_PAGE_SIZE = 20 // 페이지당 아이템 수
    }

    override fun getBookFeedItemsPaged(): Flow<PagingData<FeedItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = DEFAULT_PAGE_SIZE,
                enablePlaceholders = false // 일반적으로 false로 설정
            ),
            pagingSourceFactory = { bookLogsDao.getAllBookLogsPaged() } // DAO의 페이징 메서드 호출
        ).flow.map { pagingData ->
            pagingData.map { bookLog -> mapBookLogToFeedItem(bookLog) } // BookLog -> FeedItem 변환
        }
    }

    override fun getBookFeedItemsByMemberIdPaged(memberId: String): Flow<PagingData<FeedItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = DEFAULT_PAGE_SIZE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { bookLogsDao.getBookLogsByMemberIdPaged(memberId) }
        ).flow.map { pagingData ->
            pagingData.map { bookLog -> mapBookLogToFeedItem(bookLog) }
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
                    // DAO의 searchBookLogsPaged는 책 제목 또는 저자로 검색
                    bookLogsDao.searchBookLogsPaged(searchQuery)
                } else {
                    // 검색어가 비어있으면 모든 책을 보여줌
                    bookLogsDao.getAllBookLogsPaged()
                }
            }
        ).flow.map { pagingData ->
            pagingData.map { bookLog -> mapBookLogToFeedItem(bookLog) }
        }
    }

    override fun getAllBookFeedItemsFlow(): Flow<List<FeedItem>> {
        return bookLogsDao.getAllBookLogsFlow() // BookLogs의 Flow를 가져옴
            .map { logsList ->
                // BookLogs 리스트를 FeedItem 리스트로 변환
                logsList.map { bookLog -> mapBookLogToFeedItem(bookLog) }
            }
    }

    override suspend fun getAllBookFeedItemsList(): List<FeedItem> {
        // DAO에 Flow<List<BookLogs>>만 있으므로, Flow에서 첫 번째 값만 가져옵니다.
        val allLogsList = bookLogsDao.getAllBookLogsFlow().first() // 첫 번째 방출 값만 사용
        return allLogsList.map { bookLog -> mapBookLogToFeedItem(bookLog) }
    }

    /**
     * BookLogs 객체를 FeedItem 객체로 매핑하는 헬퍼 함수.
     * 이 함수는 BookLogs의 필드를 사용하여 FeedItem을 생성합니다.
     * "리뷰" 관련 정보는 이 컨텍스트에서 직접 사용되지 않거나, 책 정보에 맞게 단순화됩니다.
     */
    private fun mapBookLogToFeedItem(bookLog: BookLogs): FeedItem {
        // BookLogs의 createdAtMillis를 사용하여 타임스탬프 형식화 (등록일 등으로 해석)
        val sdf = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()) // 날짜만 표시하거나 필요에 맞게 변경
        val formattedTimestamp = sdf.format(Date(bookLog.createdAtMillis))

        // BookLogs의 정보를 기반으로 FeedItem 캡션 생성 (예: 책 소개, 저자 정보 등)
        // bookLog.reviewText는 "리뷰"이므로 여기서는 직접 사용하지 않거나 다른 필드(예: book_description)로 대체합니다.
        // 여기서는 책 제목과 저자로 간단한 캡션을 만듭니다.
        val captionText = "저자: ${bookLog.author ?: "정보 없음"}"

        // FeedItem의 reviews 필드는 이 컨텍스트에서는 비워두거나,
        // 책에 대한 간단한 정보(예: 평점 요약 등 BookLogs에 있다면)로 대체할 수 있습니다.
        // 여기서는 비워둡니다.
        val bookSpecificReviews = emptyList<com.peachspot.liteum.ui.screens.BookReview>() // 타입 명시

        return FeedItem(
            id = bookLog.id.toString(), // BookLogs의 ID 사용
            // userName: 이 책을 등록한 사용자 또는 관련 사용자. BookLogs의 member_id 활용.
            userName = "등록자: ${bookLog.member_id?.takeLast(4) ?: "정보 없음"}", // 임시 사용자 이름
            userProfileImageUrl = "https://picsum.photos/seed/user${bookLog.member_id?.hashCode() ?: 0}/100/100", // 임시 프로필
            postImageUrl = bookLog.coverImageUri,    // BookLogs의 표지 이미지 사용
            bookTitle = bookLog.bookTitle,           // BookLogs의 책 제목 사용
            caption = captionText,                   // 책 정보 기반 캡션
            likes = (bookLog.id % 70).toInt() + 5,   // 임시 '관심' 수 (별도 관리 필요)
            comments = (bookLog.id % 15).toInt(),  // 임시 '관련 토론' 수 (별도 관리 필요)
            timestamp = bookLog.createdAtMillis,     // BookLogs의 생성 시간 사용 (책 등록일)
            reviews = bookSpecificReviews            // 책 피드 아이템이므로, 리뷰는 비우거나 다른 정보로 대체
        )
    }
}
