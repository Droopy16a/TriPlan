package com.ramble.core.designsystem.util

/**
 * Utility to parse currency strings by extracting digits.
 */
fun parseCurrency(value: String?): Double {
    if (value == null) return 0.0
    // Heuristic: extract digits to get the whole number amount.
    // Handles "$ 2,000", "1500 USD", etc.
    return value.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
}
