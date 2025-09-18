package com.peachspot.liteum.data.remote.client

import com.peachspot.liteum.data.remote.api.GoogleBooksApiService
import com.peachspot.liteum.data.remote.api.MyApiService
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.logging.HttpLoggingInterceptor



object RetrofitClient {
    private const val BASE_URL = "https://peachspot.co.kr/ApiLitEum/search"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY // 개발 중에만 BODY, 배포 시에는 NONE 또는 BASIC
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor) // 로깅 인터셉터 추가 (선택 사항)
        .build()

    val instance: GoogleBooksApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GoogleBooksApiService::class.java)
    }
}


object NetworkClient { // 또는 AppContainer 등 앱 전체에서 접근 가능한 객체

    private const val BASE_URL = "https://peachspot.co.kr/lkf/"

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

