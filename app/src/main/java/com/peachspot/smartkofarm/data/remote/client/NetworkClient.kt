package com.peachspot.smartkofarm.data.remote.client

import com.peachspot.smartkofarm.data.remote.api.MyApiService
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkClient { // 또는 AppContainer 등 앱 전체에서 접근 가능한 객체

    private const val BASE_URL = "https://peachspot.co.kr/"

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            // 필요한 인터셉터 추가
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val myApiService: MyApiService by lazy {
        retrofit.create(MyApiService::class.java)
    }
}

