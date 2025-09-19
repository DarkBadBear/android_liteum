package com.peachspot.liteum.data.remote.api


import android.util.Log
import com.google.gson.annotations.SerializedName
import com.peachspot.liteum.data.model.BookReview
import com.peachspot.liteum.data.remote.model.BookResponse
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import javax.inject.Inject


interface MyApiService {
    @POST("ApiLitEum/delete_member")
    suspend fun deleteMemberData(
        @Header("X-Firebase-UID") firebaseUid: String?,
        @Header("X-Kakao-UID") kakaoUid: String?,
        @Query("token") token: String
    ): Response<Unit>

    @POST("ApiLitEum/register") // 엔드포인트 이름을 더 명확하게 변경
    suspend fun registerUser(
        @Header("X-Firebase-UID") firebaseUid: String?,
        @Header("X-Kakao-UID") kakaoUid: String?,
        @Query("token") token: String?,
    ): Response<Unit>

    @POST("ApiLitEum/searchBooksOnGoogle") // 엔드포인트 이름을 더 명확하게 변경
    suspend fun searchBooksOnGoogle(
        @Header("X-Firebase-UID") firebaseUid: String?,
        @Header("X-Kakao-UID") kakaoUid: String?,
        @Query("query") query: String?,
    ): BookResponse

    @GET("ApiLitEum/get_review") // 엔드포인트 경로가 올바른지 확인
    suspend fun getReviewsByIsbn(@Query("isbn") isbn: String): List<BookReview>


}

