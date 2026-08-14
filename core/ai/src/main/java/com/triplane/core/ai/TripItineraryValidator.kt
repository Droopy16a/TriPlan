package com.triplane.core.ai

import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

object TripItineraryValidator {

    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String>
    )

    fun validateChunk(
        chunk: TripItinerary,
        expectedDayRange: IntRange,
        expectedDates: List<LocalDate>
    ): ValidationResult {
        val errors = mutableListOf<String>()

        // 1. Correct number of days
        val expectedSize = expectedDayRange.last - expectedDayRange.first + 1
        if (chunk.days.size != expectedSize) {
            errors.add("Expected $expectedSize days, but got ${chunk.days.size}")
        }

        // 2. Correct day numbers
        val dayNumbers = chunk.days.map { it.dayNumber }.sorted()
        if (dayNumbers != expectedDayRange.toList()) {
            errors.add("Day numbers mismatch. Expected $expectedDayRange, got $dayNumbers")
        }

        // 3. Correct and sequential dates
        val dateStrings = chunk.days.associate { it.dayNumber to it.date }
        expectedDayRange.forEachIndexed { index, dayNum ->
            val dateStr = dateStrings[dayNum]
            if (dateStr == null) {
                errors.add("Missing date for Day $dayNum")
            } else {
                try {
                    val actualDate = LocalDate.parse(dateStr)
                    if (actualDate != expectedDates[index]) {
                        errors.add("Date mismatch for Day $dayNum: expected ${expectedDates[index]}, got $actualDate")
                    }
                } catch (e: DateTimeParseException) {
                    errors.add("Invalid date format for Day $dayNum: $dateStr")
                }
            }
        }

        // 4. Activity density & Variety
        chunk.days.forEach { day ->
            val steps = day.steps
            val stepCount = steps.size
            
            if (stepCount < 1) {
                errors.add("Day ${day.dayNumber} has no steps")
            }
            
            val activities = steps.filter { it.category.equals("Activity", ignoreCase = true) }
            
            // A full day should have at least one activity. 
            // We'll be slightly lenient for the very first/last day if they are short.
            // But if it's not day 1 or the last day, it must have activities.
            if (activities.isEmpty() && stepCount >= 3) {
                errors.add("Day ${day.dayNumber} has no activities scheduled")
            }

            // Check if it's just hotel stay
            val hotelTitles = steps.filter { it.category.equals("Accommodation", ignoreCase = true) }.map { it.title }.toSet()
            val nonHotelSteps = steps.filter { 
                !it.category.equals("Accommodation", ignoreCase = true) && 
                !hotelTitles.contains(it.title) 
            }
            
            if (nonHotelSteps.isEmpty() && stepCount > 1) {
                errors.add("Day ${day.dayNumber} only contains hotel-related steps")
            }

            if (stepCount > 10) {
                errors.add("Day ${day.dayNumber} is overloaded with $stepCount steps")
            }
        }

        // 5. Valid coordinates
        chunk.days.flatMap { it.steps }.forEach { step ->
            if (step.lat == null || step.lon == null) {
                // If it's a transport step, coordinates might be missing sometimes?
                // But user wants real POI data.
                if (step.category != "Transport" && step.category != "FreeTime") {
                    errors.add("Step '${step.title}' is missing coordinates")
                }
            }
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    fun validateFinalItinerary(
        itinerary: TripItinerary,
        expectedStartDate: LocalDate,
        expectedEndDate: LocalDate
    ): ValidationResult {
        val errors = mutableListOf<String>()
        val totalDays = ChronoUnit.DAYS.between(expectedStartDate, expectedEndDate).toInt() + 1

        if (itinerary.days.size != totalDays) {
            errors.add("Itinerary duration mismatch: expected $totalDays days, got ${itinerary.days.size}")
        }

        val dayNumbers = itinerary.days.map { it.dayNumber }
        if (dayNumbers != (1..totalDays).toList()) {
            errors.add("Day numbers are not sequential 1..$totalDays: $dayNumbers")
        }

        // Check for duplicates
        val duplicateDays = dayNumbers.groupBy { it }.filter { it.value.size > 1 }.keys
        if (duplicateDays.isNotEmpty()) {
            errors.add("Duplicate day numbers: $duplicateDays")
        }

        // Check dates
        var currentDate = expectedStartDate
        itinerary.days.forEach { day ->
            try {
                val dayDate = LocalDate.parse(day.date)
                if (dayDate != currentDate) {
                    errors.add("Date sequence break at Day ${day.dayNumber}: expected $currentDate, got $dayDate")
                }
            } catch (e: DateTimeParseException) {
                errors.add("Invalid date format at Day ${day.dayNumber}: ${day.date}")
            }
            currentDate = currentDate.plusDays(1)
        }

        return ValidationResult(errors.isEmpty(), errors)
    }
}
