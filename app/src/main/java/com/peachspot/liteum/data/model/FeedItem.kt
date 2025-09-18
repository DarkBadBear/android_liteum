package com.peachspot.liteum.data.model

// BookReview import 필요
import com.peachspot.liteum.data.model.BookReview

data class FeedItem(
    val id: String,
    val userName: String,
    val userProfileImageUrl: String?,
    val bookImageUrl: String, // 책 표지 이미지 URL
    val bookTitle: String, // 책 제목
    val caption: String,
    val likes: Int,
    val timestamp: Long,
    val reviews: List<BookReview> = emptyList() // 해당 책의 리뷰 목록
)