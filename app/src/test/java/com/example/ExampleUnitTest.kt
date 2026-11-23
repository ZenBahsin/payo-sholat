package com.example

import com.example.data.api.EquranApiClient
import com.example.data.api.EquranSholatRequest
import com.example.utils.PrayerTimesCalculator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testCompareTimes() = runBlocking {
    try {
      println("--- CALLING EQURAN API FOR PALEMBANG ---")
      val request = EquranSholatRequest(
        provinsi = "Sumatera Selatan",
        kabkota = "Kota Palembang",
        bulan = 7,
        tahun = 2026
      )
      val response = EquranApiClient.service.getMonthlySchedule(request)
      val apiTimes = response.data?.jadwal?.find { it.tanggal == 6 }
      println("API Times for July 6th:")
      println("Imsak: ${apiTimes?.imsak}")
      println("Subuh: ${apiTimes?.subuh}")
      println("Syuruq: ${apiTimes?.terbit}")
      println("Zuhur: ${apiTimes?.dzuhur}")
      println("Asar: ${apiTimes?.ashar}")
      println("Maghrib: ${apiTimes?.maghrib}")
      println("Isya: ${apiTimes?.isya}")

      println("--- MATHEMATICAL CALCULATION FOR PALEMBANG ---")
      val calculator = PrayerTimesCalculator()
      val mathTimes = calculator.calculateTimes(
        lat = -2.9909,
        lng = 104.7567,
        timezone = 7.0,
        altitude = 0.0,
        year = 2026,
        month = 7,
        day = 6,
        method = PrayerTimesCalculator.CalculationMethod.KEMENAG
      )
      println("Math Times:")
      println("Imsak: ${mathTimes.imsak}")
      println("Subuh: ${mathTimes.subuh}")
      println("Syuruq: ${mathTimes.syuruq}")
      println("Zuhur: ${mathTimes.zuhur}")
      println("Asar: ${mathTimes.asar}")
      println("Maghrib: ${mathTimes.maghrib}")
      println("Isya: ${mathTimes.isya}")

    } catch (e: Exception) {
      e.printStackTrace()
      fail("Test failed: ${e.message}")
    }
  }
}


