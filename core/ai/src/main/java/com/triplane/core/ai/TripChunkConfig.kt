package com.triplane.core.ai

import java.time.LocalDate

/**
 * Configuration constants for chunked trip generation.
 * Thresholds are intentionally in one place so they're easy to tune.
 */
object ChunkConfig {
    /** Trips up to this many days are generated in a single request. */
    const val SINGLE_REQUEST_MAX_DAYS = 5

    /** Target chunk size (days per AI request) for longer trips. */
    const val PREFERRED_CHUNK_SIZE = 4

    /** Maximum days per chunk (hard cap). */
    const val MAX_CHUNK_SIZE = 5

    /** Maximum retry attempts if a chunk fails validation. */
    const val MAX_CHUNK_RETRIES = 2

    /** Whether to run the optional final optimization pass for multi-chunk trips. */
    const val ENABLE_FINAL_OPTIMIZATION = false
}

/**
 * Describes a single generation chunk (a sub-range of the overall trip).
 */
data class TripChunk(
    val chunkNumber: Int,       // 1-based
    val totalChunks: Int,
    val startDay: Int,          // 1-based day number within the trip
    val endDay: Int,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val daysInChunk: Int = endDay - startDay + 1
)

/**
 * Splits a trip of [totalDays] into [TripChunk] descriptors.
 */
fun buildChunks(startDate: LocalDate, totalDays: Int): List<TripChunk> {
    if (totalDays <= ChunkConfig.SINGLE_REQUEST_MAX_DAYS) {
        return listOf(
            TripChunk(
                chunkNumber = 1,
                totalChunks = 1,
                startDay = 1,
                endDay = totalDays,
                startDate = startDate,
                endDate = startDate.plusDays((totalDays - 1).toLong())
            )
        )
    }

    val chunks = mutableListOf<TripChunk>()
    var dayOffset = 0
    var chunkNum = 1

    // First pass: determine chunk boundaries
    val boundaries = mutableListOf<Pair<Int, Int>>() // (startDay, endDay)
    while (dayOffset < totalDays) {
        val remaining = totalDays - dayOffset
        // Distribute remaining days as evenly as possible
        val chunkSize = when {
            remaining <= ChunkConfig.MAX_CHUNK_SIZE -> remaining
            remaining - ChunkConfig.PREFERRED_CHUNK_SIZE <= ChunkConfig.PREFERRED_CHUNK_SIZE -> {
                // If splitting would leave a tiny last chunk, make this chunk slightly larger
                (remaining + 1) / 2
            }
            else -> ChunkConfig.PREFERRED_CHUNK_SIZE
        }
        boundaries.add(Pair(dayOffset + 1, dayOffset + chunkSize))
        dayOffset += chunkSize
    }

    val totalChunks = boundaries.size
    for ((index, boundary) in boundaries.withIndex()) {
        val (startDay, endDay) = boundary
        chunks.add(
            TripChunk(
                chunkNumber = index + 1,
                totalChunks = totalChunks,
                startDay = startDay,
                endDay = endDay,
                startDate = startDate.plusDays((startDay - 1).toLong()),
                endDate = startDate.plusDays((endDay - 1).toLong())
            )
        )
    }

    return chunks
}
