package com.peachspot.liteum.data.remote.api

import com.peachspot.liteum.data.remote.model.BookResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

//
//interface GoogleBooksApiService {
//    @GET("volumes")
//    suspend fun searchBooks(
//        @Query("q") query: String, // 검색어 (예: "intitle:안드로이드 프로그래밍" 또는 "isbn:9788966262270")
//        @Header("X-Firebase-UID") firebaseUid: String?,
//        @Header("X-Kakao-UID") kakaoUid: String?,
//        @Query("maxResults") maxResults: Int = 10, // 가져올 결과 수 (기본값 및 최대값 확인 필요)
//        @Query("printType") printType: String = "books" // 책만 검색
//    ): BookResponse // BookResponse는 API 응답을 매핑할 데이터 클래스
//}