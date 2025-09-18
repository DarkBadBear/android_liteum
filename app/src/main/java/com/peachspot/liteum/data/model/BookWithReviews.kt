package com.peachspot.liteum.data.model
// BookLogs.kt 또는 별도의 파일에 정의 가능
import androidx.room.Embedded
import androidx.room.Relation
import com.peachspot.liteum.data.db.BookLogs // 여러분의 BookLogs 엔티티 경로
import com.peachspot.liteum.data.db.ReviewLogs // 여러분의 ReviewLogs 엔티티 경로

data class BookWithReviews(
    @Embedded
    val book: BookLogs, // 또는 bookLog 등 원하는 이름 사용

    @Relation(
        parentColumn = "id",        // BookLogs 엔티티의 기본 키 컬럼 이름 (로컬 id)
        entityColumn = "book_log_local_id", // ReviewLogs 엔티티에서 BookLogs를 참조하는 외래 키 컬럼 이름
        entity = ReviewLogs::class // 관련 엔티티 클래스 명시 (선택적이지만 명확성을 위해 추가)
    )
    val reviews: List<ReviewLogs> // 이 책(BookLogs)에 달린 모든 ReviewLogs 목록
)
