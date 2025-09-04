package com.peachspot.legendkofarm.data.remote.api


import com.peachspot.legendkofarm.data.core.CommonResponseData
import com.peachspot.legendkofarm.data.core.NotificationToggleRequest
import com.peachspot.legendkofarm.data.core.NotificationToggleResponse

import com.peachspot.legendkofarm.data.db.UrlData
import com.peachspot.legendkofarm.data.db.UrlEntity
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query


// 이 데이터 클래스는 API 응답이 특정 래퍼 객체로 감싸져 있을 때 사용됩니다.
// 현재 MyApiService의 메서드들이 Response<T>를 직접 반환하므로,
// 이 래퍼를 사용하려면 메서드 반환 타입을 Response<ApiResponseWrapper<T>>로 변경해야 합니다.
// (예: Response<ApiResponseWrapper<List<UrlData>>>)
// 현재 HomeRepositoryImpl은 Response<List<UrlData>> 또는 Response<UrlData>를 직접 기대하므로,
// 이 래퍼를 사용하려면 HomeRepositoryImpl도 함께 수정해야 합니다.
//data class ApiResponseWrapper<T>(
//    val data: T? // 실제 데이터, null일 수도 있음을 고려
//    // 필요한 경우 다른 공통 필드 (예: status, message 등) 추가
//)

data class ApiResponseWrapper<T>(
    val res_cd: String,
    val msg: String,
    val data: T?
)



interface MyApiService {

    @POST("legendkofarm/notification_toggle")
    suspend fun toggleNotificationState(
        @Body request: NotificationToggleRequest
    ):Response<ApiResponseWrapper<NotificationToggleResponse>>



    @POST("legendkofarm/dlrjtdlek_rgmd") // 엔드포인트 이름을 더 명확하게 변경
    suspend fun registerUser(
        @Query("uid") firebaseUid: String,
        @Query("token") token: String,
    ): Response<Unit>

    @POST("legendkofarm/drop_rgmd") // 엔드포인트 이름을 더 명확하게 변경
    suspend fun deleteMemberData(
        @Query("uid") firebaseUid: String,
        @Query("token") token: String,
    ): Response<Unit>


    @POST("legendkofarm/urls")
    suspend fun getAllUrls(
        @Query("uid") firebaseUid: String,
    ): Response<List<UrlData>> // ✅ DTO 사용

    @POST("legendkofarm/create")
    suspend fun addUrl(
        @Query("uid") firebaseUid: String,
        @Body urlData: UrlData
    ): Response<ApiResponseWrapper<UrlData>>


    @POST("legendkofarm/delete")
    suspend fun deleteUrl(
        @Query("uid") firebaseUid: String,
        @Query("id") id: Long
    ): Response<Unit>

    @POST("legendkofarm/toggle")
    suspend fun toggleSiteActive(
        @Query("uid") firebaseUid: String,
        @Query("id") id: Long
    ): Response<ApiResponseWrapper<CommonResponseData>>



    @POST("legendkofarm/update")
    suspend fun updateUrl(
        @Query("uid") firebaseUid: String,
        @Query("id") id: Long,
        @Body urlData: UrlData
    ): Response<UrlData>

    @POST("legendkofarm/check")
    suspend fun checkSiteManual(
        @Query("uid") firebaseUid: String,
        @Query("id") id: Long,
    ): Response<UrlData>

}