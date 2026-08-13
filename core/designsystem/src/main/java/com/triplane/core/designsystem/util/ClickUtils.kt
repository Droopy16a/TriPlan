package com.triplane.core.designsystem.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Executes [onClick] after a small delay to allow animations (like ripples) to finish.
 */
fun clickWithDelay(
    scope: CoroutineScope,
    delayMillis: Long = 150L,
    onClick: () -> Unit
) {
    scope.launch {
        delay(delayMillis)
        onClick()
    }
}
