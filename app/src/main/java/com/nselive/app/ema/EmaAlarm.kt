package com.nselive.app.ema

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class EmaAlarm(
    private val context: Context
) {

    companion object {

        const val CHANNEL_ID =
            "nifty_ema_alarm"

        private var notificationId =
            5000
    }

    init {
        createChannel()
    }

    private fun createChannel() {

        val sound =
            RingtoneManager.getDefaultUri(
                RingtoneManager.TYPE_ALARM
            )

        val audioAttributes =
            AudioAttributes.Builder()
                .setUsage(
                    AudioAttributes.USAGE_ALARM
                )
                .build()

        val channel =
            NotificationChannel(
                CHANNEL_ID,
                "NIFTY EMA Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )

        channel.setSound(
            sound,
            audioAttributes
        )

        channel.enableVibration(true)

        val manager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        manager.createNotificationChannel(
            channel
        )
    }

    fun trigger(
        timeframe: EmaTimeframe,
        signal: EmaSignal,
        ema9: Double,
        ema21: Double
    ) {

        if (
            android.os.Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val direction =
            when (signal) {

                EmaSignal.BULLISH ->
                    "ABOVE"

                EmaSignal.BEARISH ->
                    "BELOW"

                EmaSignal.NONE ->
                    return
            }

        val title =
            "NIFTY ${timeframe.minutes}M EMA Alert"

        val text =
            "9 EMA crossed $direction 21 EMA"

        val details =
            "$text\n" +
                    "9 EMA: %.2f\n".format(ema9) +
                    "21 EMA: %.2f".format(ema21)

        val notification =
            NotificationCompat.Builder(
                context,
                CHANNEL_ID
            )
                .setSmallIcon(
                    android.R.drawable.ic_dialog_alert
                )
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(details)
                )
                .setPriority(
                    NotificationCompat.PRIORITY_MAX
                )
                .setCategory(
                    NotificationCompat.CATEGORY_ALARM
                )
                .setAutoCancel(true)
                .setVibrate(
                    longArrayOf(
                        0,
                        700,
                        300,
                        700
                    )
                )
                .build()

        NotificationManagerCompat
            .from(context)
            .notify(
                notificationId++,
                notification
            )
    }
}
