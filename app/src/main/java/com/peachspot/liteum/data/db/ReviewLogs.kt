package com.peachspot.liteum.data.db // 또는 엔티티를 모아두는 다른 패키지

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 사용자 리뷰 정보를 담는 엔티티
 *
 * @property id 로컬 데이터베이스의 리뷰 고유 ID (자동 생성)
 * @property bookLogLocalId 이 리뷰가 달린 [BookLogs]의 로컬 ID. 외래 키로 사용될 수 있음.
 * @property bookLogServerId 이 리뷰가 달린 [BookLogs]의 서버 ID. 외래 키로 사용될 수 있음. (둘 중 하나 또는 둘 다 사용 가능)
 * @property serverReviewId 서버에 등록된 경우, 이 리뷰의 서버 ID (동기화용, 기본값 null)
 * @property reviewText 리뷰 내용
 * @property visibility 리뷰 공개 범위 (예: "PRIVATE", "PUBLIC")
 * @property createdAtMillis 리뷰 작성 시간 (epoch milliseconds)
 * @property updatedAtMillis 리뷰 수정 시간 (epoch milliseconds)
 * @property memberId 리뷰 작성자 ID
 */
@Entity(
    tableName = "user_reviews",
    foreignKeys = [
        ForeignKey(
            entity = BookLogs::class, // BookLogs 엔티티 참조
            parentColumns = ["id"],   // BookLogs의 기본 키 (로컬 id)
            childColumns = ["book_log_local_id"], // 이 테이블의 외래 키 컬럼
            onDelete = ForeignKey.CASCADE // BookLogs 항목이 삭제되면 관련된 리뷰도 삭제 (정책에 따라 SET_NULL 등 변경 가능)
        )
        // 만약 BookLogs의 server_id를 외래키로 사용하고 싶다면 추가 정의 필요
        // (단, BookLogs의 server_id가 유니크하고 인덱싱되어 있어야 효율적)
    ],
    indices = [
        Index(value = ["book_log_local_id"]), // 외래 키 컬럼에 인덱스 생성
        Index(value = ["server_review_id"], unique = true), // 서버 리뷰 ID는 유니크할 수 있음 (null 제외)
        Index(value = ["member_id"]) // 작성자 ID로 검색을 위함
    ]
)
data class ReviewLogs(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "book_log_local_id") // BookLogs 테이블의 'id'를 참조
    val bookLogLocalId: Long,

    // 필요에 따라 BookLogs의 server_id도 저장하여 연결할 수 있습니다.
    // @ColumnInfo(name = "book_log_server_id")
    // val bookLogServerId: Long?,

    @ColumnInfo(name = "server_review_id")
    val serverReviewId: Long? = null, // 서버에 등록된 리뷰의 ID (Long 타입, 기본값 null)

    @ColumnInfo(name = "review_text")
    val reviewText: String,

    @ColumnInfo(name = "visibility") // 예: "PRIVATE", "PUBLIC" 또는 0, 1 등의 Int 값
    val visibility: String, // 또는 Visibility Enum 타입을 만들고 TypeConverter 사용 가능

    @ColumnInfo(name = "created_at_millis")
    val createdAtMillis: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at_millis")
    val updatedAtMillis: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "member_id")
    val memberId: String // 리뷰 작성자 ID (non-null로 가정, 로그인 사용자만 작성 가능)
)

// 리뷰 공개 범위를 나타내는 Enum (선택 사항)
// enum class ReviewVisibility { PRIVATE, PUBLIC }
// TypeConverter를 사용하여 Room에 저장 가능
