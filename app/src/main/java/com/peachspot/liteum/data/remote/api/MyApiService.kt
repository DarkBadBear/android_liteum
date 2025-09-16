package com.peachspot.liteum.data.remote.api


import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query


data class ApiResponseWrapper<T>(
    val data: T // 실제 데이터 리스트, null일 수도 있음을 고려
    // 필요한 경우 다른 공통 필드 (예: status, message 등) 추가
)


interface MyApiService {


    @POST("lkf/delete_member")
    suspend fun deleteMemberData(
        @Query("uid") firebaseUid: String,
        @Query("token") token: String
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
//    @POST("lkf/ApRdGc") // 엔드포인트 이름을 더 명확하게 변경
//    suspend fun registerDevice(
//        @Query("tableId") tableName: String,
//        @Query("uid") firebaseUid: String,
//        @Query("token") token: String,
//    ): Response<Unit>


    @POST("lkf/dlrjtdlek_rgmd") // 엔드포인트 이름을 더 명확하게 변경
    suspend fun registerUser(
        @Query("uid") firebaseUid: String,
        @Query("token") token: String?,
    ): Response<Unit>


///// 파이어베이스 아이디를  서버로 전송함
    @POST("lkf/uploadExerciseLog") // 엔드포인트 이름을 더 명확하게 변경
    suspend fun uploadDatabaseDumpJson(
    @Query("tableId") tableName: String,
    @Query("uid") firebaseUid: String, // 사용자 식별을 위해 UID를 쿼리 파라미터로 추가하는 것을 고려
    @Body databaseDumpJson: RequestBody // String 대신 RequestBody 사용 권장 (특히 대용량일 경우)
): Response<Unit>

//
//    // 반환 타입을 ApiResponseWrapper로 변경하여 실제 JSON 구조에 맞춤
//    @GET("liteum/downExerciseLog")
//    suspend fun downExerciseJson(
//        @Query("tableId") tableName: String,
//        @Query("uid") firebaseUid: String,
//    ): Response<ApiResponseWrapper<List<ExerciseLogs>>>
//
//    @GET("liteum/downExerciseLog")
//    suspend fun downMountainJson(
//        @Query("tableId") tableName: String,
//        @Query("uid") firebaseUid: String,
//    ): Response<ApiResponseWrapper<List<MountainLog>>>
//
//    @GET("liteum/downExerciseLog")
//    suspend fun downStairJson(
//        @Query("tableId") tableName: String,
//        @Query("uid") firebaseUid: String,
//    ): Response<ApiResponseWrapper<List<StairLogs>>>
//
//    @GET("liteum/downExerciseLog")
//    suspend fun downTotalStatJson(
//        @Query("tableId") tableName: String,
//        @Query("uid") firebaseUid: String,
//    ): Response<ApiResponseWrapper<List<TotalStats>>>

}

