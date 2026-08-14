package com.example.triplane

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.triplane.feature.home.util.PlanningSignal

class PlanningNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.triplane.ACTION_CANCEL_PLANNING") {
            PlanningSignal.cancel()
        }
    }
}
