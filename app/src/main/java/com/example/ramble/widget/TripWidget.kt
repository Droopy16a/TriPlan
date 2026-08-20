package com.example.ramble.widget

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.text.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.background
import androidx.glance.appwidget.cornerRadius
import com.ramble.core.ai.TripRepository
import com.ramble.core.ai.SavedTrip
import com.example.ramble.R

class TripWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                val nextTrip = TripRepository.getNextTrip()
                TripWidgetContent(nextTrip)
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun TripWidgetContent(trip: SavedTrip?) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(16.dp)
                .cornerRadius(28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (trip == null) {
                Text(
                    text = "No upcoming trips",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = GlanceTheme.colors.onSurface
                    )
                )
            } else {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = GlanceModifier
                            .size(48.dp)
                            .background(GlanceTheme.colors.primary)
                            .cornerRadius(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = trip.emoji ?: "✈️",
                            style = TextStyle(fontSize = 20.sp)
                        )
                    }
                    Spacer(modifier = GlanceModifier.width(12.dp))
                    Column {
                        Text(
                            text = trip.title.ifBlank { trip.destination.substringBefore(",") },
                            style = TextStyle(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlanceTheme.colors.onSurface
                            )
                        )
                        Text(
                            text = trip.dates ?: "",
                            style = TextStyle(
                                fontSize = 14.sp,
                                color = GlanceTheme.colors.onSurfaceVariant
                            )
                        )
                    }
                }
                
                Spacer(modifier = GlanceModifier.height(16.dp))
                
                val daysLeft = trip.itinerary?.days?.firstOrNull()?.date?.let { dateStr ->
                    try {
                        val startDate = java.time.LocalDate.parse(dateStr)
                        java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), startDate)
                    } catch (_: Exception) { null }
                }

                if (daysLeft != null) {
                    val daysText = when {
                        daysLeft > 1 -> "$daysLeft Days Left"
                        daysLeft == 1L -> "1 Day Left"
                        daysLeft == 0L -> "Starts Today"
                        else -> "In Progress"
                    }
                    
                    Box(
                        modifier = GlanceModifier
                            .background(GlanceTheme.colors.secondaryContainer)
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .cornerRadius(16.dp)
                    ) {
                        Text(
                            text = daysText,
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlanceTheme.colors.onSecondaryContainer
                            )
                        )
                    }
                }
            }
        }
    }
}
