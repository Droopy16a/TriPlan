package com.example.ramble.widget

import android.content.Context
import com.ramble.core.ai.TripRepository
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

object WidgetUpdater {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true
        
        val appContext = context.applicationContext
        scope.launch {
            TripRepository.trips.collectLatest {
                TripWidget().updateAll(appContext)
            }
        }
    }
}
