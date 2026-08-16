package com.ramble.feature.home.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object PlanningSignal {
    private val _cancelSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val cancelSignal = _cancelSignal.asSharedFlow()

    fun cancel() {
        _cancelSignal.tryEmit(Unit)
    }
}
