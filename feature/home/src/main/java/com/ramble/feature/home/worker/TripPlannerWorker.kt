package com.ramble.feature.home.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ramble.core.ai.AiPlannerService
import com.ramble.core.ai.SavedTrip
import com.ramble.core.ai.TripRepository
import com.ramble.feature.home.util.PlanningNotificationHelper
import com.ramble.feature.home.util.emojiForDestination
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

class TripPlannerWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = coroutineScope {
        val departure = inputData.getString("departure") ?: ""
        val destination = inputData.getString("destination") ?: ""
        val startDateStr = inputData.getString("startDate")
        val endDateStr = inputData.getString("endDate")
        val travelers = inputData.getString("travelers") ?: ""
        val budget = inputData.getString("budget") ?: ""
        val preferences = inputData.getString("preferences") ?: ""

        val startDate = startDateStr?.let { try { LocalDate.parse(it) } catch(e: Exception) { null } }
        val endDate = endDateStr?.let { try { LocalDate.parse(it) } catch(e: Exception) { null } }

        val aiService = AiPlannerService(applicationContext)
        val notificationHelper = PlanningNotificationHelper(applicationContext)

        // Make this worker a foreground service so the notification stays alive 
        // even if the user swipes away the app.
        try {
            setForeground(notificationHelper.getForegroundInfo("Planning your trip to $destination…"))
        } catch (e: Exception) {
            // Log or ignore if background start is restricted
        }

        // Update notification periodically to show progress
        val progressJob = launch {
            val sequence = listOf(
                "Understanding your preferences…",
                "Exploring the best of $destination…",
                "Balancing your $budget budget…",
                "Checking travel times…",
                "Adding final touches…"
            )
            var index = 0
            while (index < sequence.size) {
                delay(4000)
                notificationHelper.showNotification(sequence[index])
                index++
            }
        }

        try {
            val result = aiService.generateTrip(departure, destination, startDate, endDate, travelers, budget, preferences)
            progressJob.cancel()
            
            if (result.isSuccess) {
                val itinerary = result.getOrThrow()
                
                val durationText = if (startDate != null && endDate != null) {
                    val days = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
                    val formatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
                    "${startDate.format(formatter)} – ${endDate.format(formatter)} ($days days)"
                } else {
                    "flexible"
                }

                val cityName = destination.substringBefore(",").trim()
                val year = startDate?.year ?: LocalDate.now().year
                val defaultTitle = "$cityName $year"

                val trip = SavedTrip(
                    id = UUID.randomUUID().toString(),
                    title = defaultTitle,
                    destination = itinerary.destination,
                    dates = durationText,
                    travelers = travelers,
                    budget = budget,
                    preferences = preferences,
                    emoji = emojiForDestination(destination),
                    itinerary = itinerary
                )
                
                TripRepository.saveSuspend(trip)
                notificationHelper.showFinalNotification(
                    title = "Trip ready ✨",
                    message = "Your adventure to $cityName is all set!"
                )
                Result.success(workDataOf("tripId" to trip.id))
            } else {
                notificationHelper.showFinalNotification(
                    title = "Planning Failed",
                    message = "We couldn't generate your trip to $destination. Please try again."
                )
                Result.failure()
            }
        } catch (e: Exception) {
            progressJob.cancel()
            if (e !is kotlinx.coroutines.CancellationException) {
                notificationHelper.showFinalNotification(
                    title = "Planning Error",
                    message = "An unexpected error occurred while planning your trip."
                )
            } else {
                notificationHelper.dismissNotification()
            }
            Result.failure()
        }
    }
}
