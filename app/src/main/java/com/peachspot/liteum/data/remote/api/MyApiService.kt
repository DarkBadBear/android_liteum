package com.peachspot.liteum.data.remote.api


import com.google.gson.annotations.SerializedName
import com.peachspot.liteum.data.remote.model.BookResponse
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query



interface GoogleBooksApiService {
    @GET("volumes")
    suspend fun searchBooks(
        @Query("q") query: String, // 검색어 (예: "intitle:안드로이드 프로그래밍" 또는 "isbn:9788966262270")
        @Header("X-Firebase-UID") firebaseUid: String?,
        @Header("X-Kakao-UID") kakaoUid: String?,
        @Query("maxResults") maxResults: Int = 10, // 가져올 결과 수 (기본값 및 최대값 확인 필요)
        @Query("printType") printType: String = "books" // 책만 검색
    ): BookResponse // BookResponse는 API 응답을 매핑할 데이터 클래스
}
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


//    @POST("liteum/update_building_data") // 실제 API 엔드포인트로 변경하세요
//    suspend fun updateBuildingData(@Body logData: ExerciseLogUpdateRequest): Response<Unit>
//
//    @POST("liteum/update_mountain_data") // 실제 API 엔드포인트로 변경하세요
//    suspend fun updateMountainData(@Body logData: MountainLogUpdateRequest): Response<Unit>
//
//    @POST("liteum/update_stair_data") // 실제 API 엔드포인트로 변경하세요
//    suspend fun updateStairData(@Body logData: StairLogUpdateRequest): Response<Unit>


    /**
     * 전체 데이터베이스 내용을 JSON 문자열 형태로 업로드합니다.
     * @param firebaseUid 사용자 식별자
     * @param databaseDumpJson 모든 테이블 데이터가 포함된 JSON 문자열
     */

}

