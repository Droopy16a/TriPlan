package com.example.ramble

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import com.ramble.feature.home.util.PlanningSignal

class PlanningNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.ramble.ACTION_CANCEL_PLANNING") {
            PlanningSignal.cancel()
            WorkManager.getInstance(context).cancelUniqueWork("trip_generation")
        }
    }
}
