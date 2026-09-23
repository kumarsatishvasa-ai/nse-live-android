package com.example.nselive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun EmaAlertScreen(
    currentPrice: Double,
    ema9: Double?,
    ema21: Double?,
    alertEnabled: Boolean,
    onAlertEnabledChanged: (Boolean) -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            text = "NIFTY EMA Alert"
        )

        Text(
            text = "NIFTY: %.2f".format(currentPrice)
        )

        Text(
            text = if (ema9 != null)
                "9 EMA: %.2f".format(ema9)
            else
                "9 EMA: --"
        )

        Text(
            text = if (ema21 != null)
                "21 EMA: %.2f".format(ema21)
            else
                "21 EMA: --"
        )

        if (ema9 != null && ema21 != null) {

            Text(
                text = if (ema9 > ema21)
                    "Trend: 9 EMA ABOVE 21 EMA"
                else
                    "Trend: 9 EMA BELOW 21 EMA"
            )
        }

        Text(
            text = "EMA Crossover Alarm"
        )

        Switch(
            checked = alertEnabled,
            onCheckedChange = onAlertEnabledChanged
        )
    }
}
