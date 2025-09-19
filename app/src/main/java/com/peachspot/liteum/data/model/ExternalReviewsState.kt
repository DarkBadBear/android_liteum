package com.peachspot.liteum.data.model


data class ExternalReviewsState(
    val loading: Boolean = false,
    val reviews: List<BookReview>? = null,
    val error: String? = null
)