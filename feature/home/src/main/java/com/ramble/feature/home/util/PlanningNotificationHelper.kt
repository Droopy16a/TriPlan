package com.ramble.feature.home.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.ForegroundInfo
import com.ramble.core.designsystem.R as DesignR

class PlanningNotificationHelper(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "trip_planning_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_CANCEL = "com.ramble.ACTION_CANCEL_PLANNING"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Trip Planning"
            val descriptionText = "Shows progress when planning a trip"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun getForegroundInfo(message: String): ForegroundInfo {
        val notification = createNotification(message, isOngoing = true, showProgress = true)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    fun showNotification(message: String) {
        try {
            NotificationManagerCompat.from(context).notify(
                NOTIFICATION_ID, 
                createNotification(message, isOngoing = true, showProgress = true)
            )
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }

    fun showFinalNotification(title: String, message: String) {
        try {
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(DesignR.drawable.ic_stat_name)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true) // Dismiss when tapped
                .setOngoing(false) // Can be swiped away

            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }

    private fun createNotification(message: String, isOngoing: Boolean, showProgress: Boolean): android.app.Notification {
        val cancelIntent = Intent(ACTION_CANCEL).apply {
            setPackage(context.packageName)
        }
        val cancelPendingIntent = PendingIntent.getBroadcast(
            context, 0, cancelIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(DesignR.drawable.ic_stat_name)
            .setContentTitle("Planning your trip")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(isOngoing)

        if (showProgress) {
            builder.setProgress(0, 0, true)
        }
        
        if (isOngoing) {
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPendingIntent)
        }

        return builder.build()
    }

    fun dismissNotification() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }
}
