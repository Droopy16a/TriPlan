package com.ramble.core.ai.providers

import com.ramble.core.ai.models.WeatherInfo
import java.time.LocalDate

interface WeatherProvider {
    suspend fun getForecast(lat: Double, lon: Double, startDate: LocalDate?, endDate: LocalDate?): WeatherInfo
}
