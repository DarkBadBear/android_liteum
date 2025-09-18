package com.peachspot.liteum.data.model

data class BookReview(
    val id: String,
    val userId: String, // 리뷰를 작성한 사용자의 고유 ID
    val reviewerName: String, // 리뷰 작성자의 표시 이름 (userId와 별개로 유지 가능)
    val reviewText: String,
    val rating: Float ,// 0.0 ~ 5.0
    val content: String, // <--- 이 부분이 중요합니다!
    val timestamp: Long = System.currentTimeMillis()
)