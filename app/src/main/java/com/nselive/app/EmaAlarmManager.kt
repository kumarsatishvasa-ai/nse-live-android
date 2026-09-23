package com.example.nselive

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class EmaAlarmManager(
    private val context: Context
) {

    companion object {
        private const val CHANNEL_ID = "ema_alerts"
        private const val NOTIFICATION_ID = 9001
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {

        val soundUri =
            RingtoneManager.getDefaultUri(
                RingtoneManager.TYPE_NOTIFICATION
            )

        val channel = NotificationChannel(
            CHANNEL_ID,
            "NIFTY EMA Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {

            description = "NIFTY 9 EMA / 21 EMA crossover alerts"

            setSound(
                soundUri,
                android.media.AudioAttributes.Builder()
                    .setUsage(
                        android.media.AudioAttributes.USAGE_NOTIFICATION
                    )
                    .build()
            )

            enableVibration(true)
        }

        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager

        manager.createNotificationChannel(channel)
    }

    fun showBullishAlert(
        ema9: Double,
        ema21: Double
    ) {

        showNotification(
            title = "NIFTY Bullish EMA Crossover",
            message = "9 EMA crossed ABOVE 21 EMA\n" +
                    "9 EMA: %.2f\n21 EMA: %.2f"
                        .format(ema9, ema21)
        )
    }

    fun showBearishAlert(
        ema9: Double,
        ema21: Double
    ) {

        showNotification(
            title = "NIFTY Bearish EMA Crossover",
            message = "9 EMA crossed BELOW 21 EMA\n" +
                    "9 EMA: %.2f\n21 EMA: %.2f"
                        .format(ema9, ema21)
        )
    }

    private fun showNotification(
        title: String,
        message: String
    ) {

        if (
            android.os.Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notification =
            NotificationCompat.Builder(
                context,
                CHANNEL_ID
            )
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(message)
                )
                .setPriority(
                    NotificationCompat.PRIORITY_HIGH
                )
                .setAutoCancel(true)
                .setVibrate(longArrayOf(0, 500, 300, 500))
                .build()

        NotificationManagerCompat
            .from(context)
            .notify(
                NOTIFICATION_ID,
                notification
            )
    }
}
