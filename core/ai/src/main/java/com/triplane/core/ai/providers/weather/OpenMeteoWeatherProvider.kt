package com.triplane.core.ai.providers.weather

import com.triplane.core.ai.models.WeatherInfo
import com.triplane.core.ai.providers.WeatherProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Serializable
private data class OpenMeteoResponse(
    val daily: DailyForecast? = null
)

@Serializable
private data class DailyForecast(
    val time: List<String> = emptyList(),
    val temperature_2m_max: List<Double?> = emptyList(),
    val temperature_2m_min: List<Double?> = emptyList(),
    val weathercode: List<Int?> = emptyList()
)

class OpenMeteoWeatherProvider(private val httpClient: HttpClient) : WeatherProvider {
    
    override suspend fun getForecast(lat: Double, lon: Double, startDate: LocalDate?, endDate: LocalDate?): WeatherInfo {
        val start = startDate ?: LocalDate.now()
        val end = endDate ?: start.plusDays(3)
        
        return try {
            val response: OpenMeteoResponse = httpClient.get("https://api.open-meteo.com/v1/forecast") {
                parameter("latitude", lat)
                parameter("longitude", lon)
                parameter("daily", "weathercode,temperature_2m_max,temperature_2m_min")
                parameter("timezone", "auto")
                parameter("start_date", start.format(DateTimeFormatter.ISO_LOCAL_DATE))
                parameter("end_date", end.format(DateTimeFormatter.ISO_LOCAL_DATE))
            }.body()
            
            val forecastSummary = if (response.daily != null && response.daily.time.isNotEmpty()) {
                val dailyStr = response.daily.time.mapIndexed { index, date ->
                    val max = response.daily.temperature_2m_max.getOrNull(index) ?: "?"
                    val min = response.daily.temperature_2m_min.getOrNull(index) ?: "?"
                    val code = response.daily.weathercode.getOrNull(index) ?: "?"
                    
                    "$date: Max $max°C, Min $min°C, WeatherCode $code"
                }.joinToString("\n")
                
                "Forecast:\n$dailyStr"
            } else {
                "Weather data unavailable."
            }
            
            WeatherInfo(
                destination = "$lat,$lon",
                startDate = start.toString(),
                endDate = end.toString(),
                forecast = forecastSummary
            )
        } catch (e: Exception) {
            e.printStackTrace()
            WeatherInfo("$lat,$lon", start.toString(), end.toString(), "Weather data unavailable.")
        }
    }
}
