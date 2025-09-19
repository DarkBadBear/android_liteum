package com.peachspot.liteum.data.remote.model // 실제 프로젝트 구조에 맞게 패키지명 변경

import com.google.gson.annotations.SerializedName // Gson 사용 시
// import kotlinx.serialization.SerialName // Kotlinx Serialization 사용 시
// import kotlinx.serialization.Serializable // Kotlinx Serialization 사용 시

// @Serializable // Kotlinx Serialization 사용 시
data class BookResponse(
    @SerializedName("items") // Gson 사용 시
    // @SerialName("items") // Kotlinx Serialization 사용 시
    val items: List<BookItem>? = null, // API 응답에 따라 items가 없을 수도 있음

    @SerializedName("kind")
    val kind: String? = null,

    @SerializedName("totalItems")
    val totalItems: Int? = null
)

// @Serializable // Kotlinx Serialization 사용 시
data class BookItem(
    @SerializedName("kind")
    val kind: String? = null,

    @SerializedName("id")
    val id: String? = null,

    @SerializedName("etag")
    val etag: String? = null,

    @SerializedName("selfLink")
    val selfLink: String? = null,

    // 바로 이 필드입니다!
    @SerializedName("volumeInfo")
    val volumeInfo: VolumeInfo? = null, // API 응답에 따라 volumeInfo가 없을 수도 있음

    @SerializedName("saleInfo")
    val saleInfo: SaleInfo? = null,

    @SerializedName("accessInfo")
    val accessInfo: AccessInfo? = null,

    @SerializedName("searchInfo")
    val searchInfo: SearchInfo? = null
)

// @Serializable // Kotlinx Serialization 사용 시
data class VolumeInfo(
    @SerializedName("title")
    val title: String? = null,

    @SerializedName("authors")
    val authors: List<String>? = null,

    @SerializedName("publisher")
    val publisher: String? = null,

    @SerializedName("publishedDate")
    val publishedDate: String? = null,

    @SerializedName("description")
    val description: String? = null,

    @SerializedName("industryIdentifiers")
    val industryIdentifiers: List<IndustryIdentifier>? = null,

    @SerializedName("readingModes")
    val readingModes: ReadingModes? = null,

    @SerializedName("pageCount")
    val pageCount: Int? = null,

    @SerializedName("printType")
    val printType: String? = null,

    @SerializedName("categories")
    val categories: List<String>? = null,

    @SerializedName("averageRating")
    val averageRating: Double? = null,

    @SerializedName("ratingsCount")
    val ratingsCount: Int? = null,

    @SerializedName("maturityRating")
    val maturityRating: String? = null,

    @SerializedName("allowAnonLogging")
    val allowAnonLogging: Boolean? = null,

    @SerializedName("contentVersion")
    val contentVersion: String? = null,

    @SerializedName("panelizationSummary")
    val panelizationSummary: PanelizationSummary? = null,

    @SerializedName("imageLinks")
    val imageLinks: ImageLinks? = null,

    @SerializedName("language")
    val language: String? = null,

    @SerializedName("previewLink")
    val previewLink: String? = null,

    @SerializedName("infoLink")
    val infoLink: String? = null,

    @SerializedName("canonicalVolumeLink")
    val canonicalVolumeLink: String? = null
)

// @Serializable // Kotlinx Serialization 사용 시
data class IndustryIdentifier(
    @SerializedName("type")
    val type: String? = null, // 예: "ISBN_13", "ISBN_10"

    @SerializedName("identifier")
    val identifier: String? = null
)

// @Serializable // Kotlinx Serialization 사용 시
data class ImageLinks(
    @SerializedName("smallThumbnail")
    val smallThumbnail: String? = null,

    @SerializedName("thumbnail")
    val thumbnail: String? = null
)

// 기타 필요한 데이터 클래스들 (SaleInfo, AccessInfo, SearchInfo, ReadingModes, PanelizationSummary 등)
// API 응답에 맞춰 필요한 만큼만 정의하거나, 전부 정의할 수 있습니다.
// 예시:
// @Serializable
data class SaleInfo(
    @SerializedName("country")
    val country: String? = null,
    /* ... */
)

// @Serializable
data class AccessInfo(
    @SerializedName("country")
    val country: String? = null,
    /* ... */
)

// @Serializable
data class SearchInfo(
    @SerializedName("textSnippet")
    val textSnippet: String? = null
)

// @Serializable
data class ReadingModes(
    @SerializedName("text")
    val text: Boolean? = null,
    @SerializedName("image")
    val image: Boolean? = null
)

// @Serializable
data class PanelizationSummary(
    @SerializedName("containsEpubBubbles")
    val containsEpubBubbles: Boolean? = null,
    @SerializedName("containsImageBubbles")
    val containsImageBubbles: Boolean? = null
)

data class ApiResponseWrapper<T>(
    val data: T // 실제 데이터 리스트, null일 수도 있음을 고려
    // 필요한 경우 다른 공통 필드 (예: status, message 등) 추가
)



// @Serializable // Kotlinx Serialization 사용 시 주석 해제
data class BookReview(
    @SerializedName("review_id") // JSON 키가 "review_id"인 경우
    val id: String, // 로컬 모델과 이름이 같다면 어노테이션 불필요

    @SerializedName("user_id")
    val userId: String,

    @SerializedName("book_isbn") // API 응답에 ISBN이 포함된다면
    val bookId: String, // 또는 isbn: String, 로컬 모델과 일치시킴

    @SerializedName("rating")
    val rating: Float,

    @SerializedName("content")
    val content: String,

    @SerializedName("timestamp") // 타임스탬프 형식에 따라 Long 또는 String
    val timestamp: Long,

    @SerializedName("image_url")
    val imageUrl: String? = null, // nullable 필드

    @SerializedName("likes_count")
    val likes: Int? = 0 // nullable 필드 및 기본값
    // ... 기타 필요한 필드들
)
