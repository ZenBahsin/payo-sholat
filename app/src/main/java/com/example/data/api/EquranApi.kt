package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class EquranSholatRequest(
    @Json(name = "provinsi") val provinsi: String,
    @Json(name = "kabkota") val kabkota: String,
    @Json(name = "bulan") val bulan: Int,
    @Json(name = "tahun") val tahun: Int
)

@JsonClass(generateAdapter = true)
data class EquranSholatResponse(
    @Json(name = "code") val code: Int,
    @Json(name = "message") val message: String,
    @Json(name = "data") val data: EquranData?
)

@JsonClass(generateAdapter = true)
data class EquranData(
    @Json(name = "provinsi") val provinsi: String,
    @Json(name = "kabkota") val kabkota: String,
    @Json(name = "bulan") val bulan: Int,
    @Json(name = "tahun") val tahun: Int,
    @Json(name = "bulan_nama") val bulan_nama: String,
    @Json(name = "jadwal") val jadwal: List<EquranJadwal>
)

@JsonClass(generateAdapter = true)
data class EquranJadwal(
    @Json(name = "tanggal") val tanggal: Int,
    @Json(name = "tanggal_lengkap") val tanggal_lengkap: String,
    @Json(name = "hari") val hari: String,
    @Json(name = "imsak") val imsak: String,
    @Json(name = "subuh") val subuh: String,
    @Json(name = "terbit") val terbit: String,
    @Json(name = "dhuha") val dhuha: String,
    @Json(name = "dzuhur") val dzuhur: String,
    @Json(name = "ashar") val ashar: String,
    @Json(name = "maghrib") val maghrib: String,
    @Json(name = "isya") val isya: String
)

@JsonClass(generateAdapter = true)
data class ApiSurahItem(
    @Json(name = "nomor") val nomor: Int,
    @Json(name = "nama") val nama: String,
    @Json(name = "namaLatin") val namaLatin: String,
    @Json(name = "jumlahAyat") val jumlahAyat: Int,
    @Json(name = "tempatTurun") val tempatTurun: String,
    @Json(name = "arti") val arti: String,
    @Json(name = "deskripsi") val deskripsi: String,
    @Json(name = "audioFull") val audioFull: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class ApiAyatItem(
    @Json(name = "nomorAyat") val nomorAyat: Int,
    @Json(name = "teksArab") val teksArab: String,
    @Json(name = "teksLatin") val teksLatin: String,
    @Json(name = "teksIndonesia") val teksIndonesia: String,
    @Json(name = "audio") val audio: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class ApiSurahDetail(
    @Json(name = "nomor") val nomor: Int,
    @Json(name = "nama") val nama: String,
    @Json(name = "namaLatin") val namaLatin: String,
    @Json(name = "jumlahAyat") val jumlahAyat: Int,
    @Json(name = "tempatTurun") val tempatTurun: String,
    @Json(name = "arti") val arti: String,
    @Json(name = "deskripsi") val deskripsi: String,
    @Json(name = "audioFull") val audioFull: Map<String, String>? = null,
    @Json(name = "ayat") val ayat: List<ApiAyatItem>
)

@JsonClass(generateAdapter = true)
data class EquranSurahListResponse(
    @Json(name = "code") val code: Int,
    @Json(name = "message") val message: String,
    @Json(name = "data") val data: List<ApiSurahItem>?
)

@JsonClass(generateAdapter = true)
data class EquranSurahDetailResponse(
    @Json(name = "code") val code: Int,
    @Json(name = "message") val message: String,
    @Json(name = "data") val data: ApiSurahDetail?
)

interface EquranApiService {
    @POST("api/v2/shalat")
    suspend fun getMonthlySchedule(@Body request: EquranSholatRequest): EquranSholatResponse

    @retrofit2.http.GET("api/v2/surat")
    suspend fun getSurahList(): EquranSurahListResponse

    @retrofit2.http.GET("api/v2/surat/{nomor}")
    suspend fun getSurahDetail(@retrofit2.http.Path("nomor") nomor: Int): EquranSurahDetailResponse
}

object EquranApiClient {
    private const val BASE_URL = "https://equran.id/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val service: EquranApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(EquranApiService::class.java)
    }
}
