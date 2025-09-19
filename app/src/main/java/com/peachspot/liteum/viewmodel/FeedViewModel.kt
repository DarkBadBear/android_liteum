package com.peachspot.liteum.viewmodel

import androidx.activity.result.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.peachspot.liteum.data.db.BookLogsDao

import com.peachspot.liteum.data.model.FeedItem // UI 모델
import com.peachspot.liteum.data.model.BookReview // UI 모델
import com.peachspot.liteum.data.model.BookWithReviews
import com.peachspot.liteum.data.model.ExternalReviewsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch


class FeedViewModelFactory(
    private val bookLogsDao: BookLogsDao,
    private val bookApiService: BookApiService // 인터페이스 타입으로 받음
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FeedViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FeedViewModel(bookLogsDao, bookApiService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class for FeedViewModelFactory")
    }
}

class FeedViewModel(
    private val bookLogsDao: BookLogsDao,
    private val bookApiService: BookApiService // 기본값 할당 제거!
) : ViewModel() {

    // BookWithReviews (DB 데이터)를 FeedItem (UI 모델)으로 변환하는 핵심 함수
    private fun mapBookWithReviewsToFeedItem(bookWithReviews: BookWithReviews): FeedItem {
        val bookLog = bookWithReviews.book // 실제 책 정보 (BookLogs 객체)
        val reviewLogsList = bookWithReviews.reviews // 해당 책에 달린 모든 리뷰 (List<ReviewLogs>)

        // FeedItem의 ID는 이제 BookLogs의 로컬 id만 사용
        val feedItemId = bookLog.id.toString()

        // DB에서 가져온 List<ReviewLogs>를 UI 모델인 List<BookReview>로 변환
        val uiBookReviews = reviewLogsList.map { reviewLogDb ->
            // ReviewLogs (DB 엔티티) -> BookReview (UI 모델) 매핑
            BookReview(
                id = reviewLogDb.id.toString(), // ReviewLogs의 로컬 ID를 사용
                userId = reviewLogDb.memberId,
                reviewerName = reviewLogDb.memberId, // 실제 앱에서는 사용자 프로필에서 가져오는 것을 고려
                reviewText = reviewLogDb.reviewText,
                rating = 0.0f, // ReviewLogs 엔티티에 rating 필드가 없으므로 기본값 사용.
                // 또는 BookReview UI 모델의 rating을 Float?로 하고 null 전달 가능.
                content = reviewLogDb.reviewText, // 중요한 'content' 필드에 reviewText 사용
                timestamp = reviewLogDb.createdAtMillis
            )
        }

        // FeedItem 객체 생성
        return FeedItem(
            id = feedItemId,
            userName = bookLog.member_id ?: "익명 사용자",
            userProfileImageUrl = null, // BookLogs에 사용자 프로필 이미지 URL 필드가 현재 없음 (임시로 null)
            // 필요하다면 "https://picsum.photos/seed/user${bookLog.member_id?.hashCode() ?: 0}/100/100" 등 임시 URL 사용
            bookImageUrl = bookLog.coverImageUri,
            bookTitle = bookLog.bookTitle,
            caption = "제목: ${bookLog.bookTitle}", // 임시 caption
            // likes 필드 추가 (임시값 또는 실제 데이터 로직 필요)
            likes = (bookLog.id % 70).toInt() + 5, // 예시: ID 기반으로 임의의 좋아요 수 생성
            timestamp = bookLog.createdAtMillis,
            reviews = uiBookReviews,
            isbn =bookLog.isbn
        )
    }

    /**
     * 페이징된 피드 아이템(책과 그 리뷰들)을 제공하는 Flow.
     * 각 아이템은 FeedItem UI 모델입니다.
     */
    val feedItemsPager: Flow<PagingData<FeedItem>> = Pager(
        config = PagingConfig(
            pageSize = 10, // 한 페이지에 로드할 책(FeedItem)의 수
            enablePlaceholders = false
        ),
        pagingSourceFactory = { bookLogsDao.getAllBooksWithReviewsPaged() } // DAO 메서드 사용
    ).flow
        .map { pagingDataBookWithReviews: PagingData<BookWithReviews> ->
            pagingDataBookWithReviews.map { bookWithReviews ->
                mapBookWithReviewsToFeedItem(bookWithReviews) // 각 BookWithReviews를 FeedItem으로 변환
            }
        }
        .cachedIn(viewModelScope) // ViewModel 스코프 내에서 페이징 데이터 캐시

    private val _externalReviews = MutableStateFlow<Map<String, ExternalReviewsState>>(emptyMap())
    val externalReviews: StateFlow<Map<String, ExternalReviewsState>> = _externalReviews.asStateFlow()

    fun fetchExternalReviews(feedItemId: String, isbn: String) {
        if (_externalReviews.value[feedItemId]?.loading == true || _externalReviews.value[feedItemId]?.reviews != null) {
            return
        }

        viewModelScope.launch {
            _externalReviews.value = _externalReviews.value.toMutableMap().apply {
                put(feedItemId, ExternalReviewsState(loading = true))
            }
            try {
                val reviews = bookApiService.getReviewsByIsbn(isbn)
                _externalReviews.value = _externalReviews.value.toMutableMap().apply {
                    put(feedItemId, ExternalReviewsState(reviews = reviews))
                }
            } catch (e: Exception) {
                _externalReviews.value = _externalReviews.value.toMutableMap().apply {
                    put(feedItemId, ExternalReviewsState(error = e.message))
                }
            }
        }
    }
}
