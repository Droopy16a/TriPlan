package com.ramble.feature.home.util

fun emojiForDestination(destination: String): String {
    val lower = destination.lowercase()
    return when {
        lower.contains("japan") || lower.contains("tokyo") || lower.contains("kyoto") -> "⛩️"
        lower.contains("paris") || lower.contains("france") -> "🗼"
        lower.contains("new york") || lower.contains("usa") || lower.contains("america") -> "🗽"
        lower.contains("london") || lower.contains("uk") || lower.contains("england") -> "🎡"
        lower.contains("rome") || lower.contains("italy") -> "🏛️"
        lower.contains("barcelona") || lower.contains("spain") -> "💃"
        lower.contains("dubai") -> "🏙️"
        lower.contains("bali") || lower.contains("indonesia") -> "🌴"
        else -> "✈️"
    }
}
