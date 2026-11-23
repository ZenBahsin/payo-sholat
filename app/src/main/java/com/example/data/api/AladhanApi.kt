package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class AladhanGToHResponse(
    @Json(name = "code") val code: Int,
    @Json(name = "status") val status: String,
    @Json(name = "data") val data: AladhanGToHData?
)

@JsonClass(generateAdapter = true)
data class AladhanGToHData(
    @Json(name = "hijri") val hijri: AladhanHijri?
)

@JsonClass(generateAdapter = true)
data class AladhanHijri(
    @Json(name = "day") val day: String,
    @Json(name = "month") val month: AladhanMonth?,
    @Json(name = "year") val year: String
)

@JsonClass(generateAdapter = true)
data class AladhanMonth(
    @Json(name = "number") val number: Int,
    @Json(name = "en") val en: String,
    @Json(name = "ar") val ar: String
)

interface AladhanApiService {
    @retrofit2.http.GET("v1/gToH/{date}")
    suspend fun getHijriDate(@retrofit2.http.Path("date") date: String): AladhanGToHResponse
}

object AladhanApiClient {
    private const val BASE_URL = "https://api.aladhan.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val service: AladhanApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(AladhanApiService::class.java)
    }
}
