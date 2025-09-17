package com.peachspot.liteum.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters // 필요한 경우 TypeConverter 유지 또는 추가
// import com.peachspot.liteum.util.TimestampListConverter // List<Long> 등이 없다면 제거 가능

@Entity(tableName = "book_reviews") // 테이블 이름도 "book_reviews" 등으로 변경하는 것이 좋습니다.
// @TypeConverters(...) // List<String> 같은 복잡한 타입을 사용한다면 TypeConverter 필요
data class BookLogs( // 클래스 이름을 BookReview 등으로 변경하는 것을 고려해보세요.
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "server_id", defaultValue = "0") // 서버의 책장 ID, 기본값 0
    val serverId: Long = 0L,

    // 필수 입력
    @ColumnInfo(name = "book_title")
    val bookTitle: String,         // 책 제목

    @ColumnInfo(name = "cover_image_uri") // 로컬 파일 경로(Uri 문자열) 또는 원격 URL
    val coverImageUri: String,     // 사진 Uri 문자열

    // 선택 입력
    @ColumnInfo(name = "start_read_date") // "YYYY-MM-DD" 형식의 문자열 또는 Long (타임스탬프)
    val startReadDate: String?,    // 읽기 시작한 날

    @ColumnInfo(name = "end_read_date")   // "YYYY-MM-DD" 형식의 문자열 또는 Long (타임스탬프)
    val endReadDate: String?,      // 다 읽은 날

    @ColumnInfo(name = "author")
    val author: String?,           // 책 저자

    @ColumnInfo(name = "member_id")
    val member_id: String?,           // 작성자아이디


    @ColumnInfo(name = "book_genre") // 또는 book_category
    val bookGenre: String?,        // 책 종류 (장르)

    @ColumnInfo(name = "publish_date")    // "YYYY-MM-DD" 형식의 문자열 또는 Long (타임스탬프)
    val publishDate: String?,      // 발행일

    @ColumnInfo(name = "isbn")
    val isbn: String?,             // ISBN

    // 추가적으로 필요한 정보 (예시)
    @ColumnInfo(name = "rating")
    val rating: Float?,            // 별점 (0.0 ~ 5.0)

    @ColumnInfo(name = "review_text")
    val reviewText: String?,       // 한줄평 또는 상세 리뷰 내용

    @ColumnInfo(name = "page_count")
    val pageCount: Int?,           // 책 페이지 수 (API로 가져올 수 있다면)

    @ColumnInfo(name = "publisher")
    val publisher: String?,        // 출판사 (API로 가져올 수 있다면)

    @ColumnInfo(name = "created_at_millis") // 기록 생성 시간
    val createdAtMillis: Long = System.currentTimeMillis()
)
