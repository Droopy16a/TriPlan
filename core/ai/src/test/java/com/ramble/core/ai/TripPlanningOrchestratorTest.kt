package com.ramble.core.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class TripPlanningOrchestratorTest {

    @Test
    fun testBuildDeterministicSummary() {
        val orchestrator = TripPlanningOrchestrator()

        val days = listOf(
            TripDay(
                dayNumber = 1,
                date = "2026-09-01",
                theme = "Historical Center",
                steps = emptyList()
            ),
            TripDay(
                dayNumber = 2,
                date = "2026-09-02",
                theme = "Art & Museums",
                steps = emptyList()
            )
        )

        val summary = orchestrator.buildDeterministicSummary("Florence", days)
        assertEquals("A 2-days journey exploring Florence, featuring Historical Center, Art & Museums.", summary)
    }

    @Test
    fun testComputeBudgetAllocation() {
        val orchestrator = TripPlanningOrchestrator()

        val days = listOf(
            TripDay(
                dayNumber = 1,
                date = "2026-09-01",
                theme = "Exploring",
                steps = listOf(
                    TripStep(time = "09:00", title = "Hotel", description = "", category = "Accommodation", estimatedCost = 100.0),
                    TripStep(time = "12:00", title = "Bistro", description = "", category = "Food", estimatedCost = 50.0),
                    TripStep(time = "14:00", title = "Bus", description = "", category = "Transport", estimatedCost = 10.0),
                    TripStep(time = "15:00", title = "Museum", description = "", category = "Activity", estimatedCost = 40.0)
                )
            )
        )

        // Total cost = 100 + 50 + 10 + 40 = 200
        // Accommodation = 100 / 200 = 50%
        // Food = 50 / 200 = 25%
        // Transport = 10 / 200 = 5%
        // Activities = 40 / 200 = 20%
        val allocation = orchestrator.computeBudgetAllocation(days)
        assertEquals("50%", allocation.accommodation)
        assertEquals("25%", allocation.food)
        assertEquals("5%", allocation.transport)
        assertEquals("20%", allocation.activities)
    }
}
