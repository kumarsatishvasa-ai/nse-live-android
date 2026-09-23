package com.example.nselive.ema

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class EmaNotificationManager(
    private val context: Context
) {

    companion object {

        const val CHANNEL_ID =
            "nifty_ema_alerts"

        private const val CHANNEL_NAME =
            "NIFTY EMA Alerts"

        private var notificationId = 1000
    }

    init {
        createChannel()
    }

    private fun createChannel() {

        val sound =
            RingtoneManager.getDefaultUri(
                RingtoneManager.TYPE_ALARM
            )

        val attributes =
            AudioAttributes.Builder()
                .setUsage(
                    AudioAttributes.USAGE_ALARM
                )
                .build()

        val channel =
            NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )

        channel.description =
            "NIFTY 9 EMA / 21 EMA crossover alerts"

        channel.setSound(
            sound,
            attributes
        )

        channel.enableVibration(true)

        val manager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        manager.createNotificationChannel(channel)
    }

    fun show(
        timeframe: TimeFrame,
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

        val bullish =
            signal == EmaSignal.BULLISH

        val direction =
            if (bullish) "ABOVE"
            else "BELOW"

        val title =
            "NIFTY ${timeframe.minutes}M EMA Alert"

        val message =
            "9 EMA crossed $direction 21 EMA\n" +
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
                .setContentText(
                    "9 EMA crossed $direction 21 EMA"
                )
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(message)
                )
                .setPriority(
                    NotificationCompat.PRIORITY_MAX
                )
                .setAutoCancel(true)
                .setCategory(
                    NotificationCompat.CATEGORY_ALARM
                )
                .setVibrate(
                    longArrayOf(
                        0,
                        500,
                        300,
                        500
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
