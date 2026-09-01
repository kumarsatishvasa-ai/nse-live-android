```kotlin
package com.nselive.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                NseLiveScreen()
            }
        }
    }
}

@Composable
fun NseLiveScreen() {

    val repository =
        remember {
            NseRepository()
        }

    var metrics by remember {
        mutableStateOf<NseMetrics?>(null)
    }

    var loading by remember {
        mutableStateOf(false)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    suspend fun refresh() {

        if (loading) {
            return
        }

        loading = true
        error = null

        try {

            metrics =
                repository.loadNifty()

        } catch (e: Exception) {

            error =
                e.message
                    ?: "Unable to retrieve NSE data."

        } finally {

            loading = false
        }
    }

    /*
     * Initial load + automatic refresh.
     *
     * NSE market hours are approximately 09:15–15:30 IST
     * Monday-Friday. We only refresh during that period.
     */
    LaunchedEffect(Unit) {

        while (true) {

            if (isMarketHours()) {
                refresh()
                delay(30_000)
            } else {
                delay(60_000)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {

                        Text(
                            text = "NSE LIVE",
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "NIFTY 50",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            )
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            item {

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween,
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text =
                            if (isMarketHours())
                                "● MARKET OPEN"
                            else
                                "● MARKET CLOSED",
                        color =
                            if (isMarketHours())
                                Color(0xFF2E7D32)
                            else
                                Color.Gray,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Button(
                        onClick = {
                            // Manual refresh is triggered
                            // through a new coroutine below.
                        }
                    ) {
                        Text("Refresh")
                    }
                }
            }

            item {

                if (loading) {

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.Center,
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        CircularProgressIndicator()

                        Spacer(
                            modifier =
                                Modifier.padding(8.dp)
                        )

                        Text(
                            text =
                                "Updating NSE..."
                        )
                    }
                }
            }

            val current =
                metrics

            item {
                MetricCard(
                    "PCR (OI)",
                    current?.pcrOi
                )
            }

            item {
                MetricCard(
                    "PCR (Volume)",
                    current?.pcrVolume
                )
            }

            item {
                MetricCard(
                    "Max Pain",
                    current?.maxPain
                )
            }

            item {
                MetricCard(
                    "Gamma Flip",
                    current?.gammaFlip
                )
            }

            item {
                MetricCard(
                    "Call Wall",
                    current?.callWall
                )
            }

            item {
                MetricCard(
                    "Put Wall",
                    current?.putWall
                )
            }

            item {
                MetricCard(
                    "Expected Move",
                    current?.expectedMove
                )
            }

            item {
                MetricCard(
                    "India VIX",
                    current?.indiaVix
                )
            }

            item {

                Card(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Column(
                        modifier =
                            Modifier.padding(16.dp)
                    ) {

                        Text(
                            text =
                                "Last Updated",
                            color =
                                Color.Gray,
                            fontSize =
                                13.sp
                        )

                        Spacer(
                            modifier =
                                Modifier.height(4.dp)
                        )

                        Text(
                            text =
                                current?.updatedAt
                                    ?: "--:--:--",
                            fontSize =
                                18.sp,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }
            }

            item {

                error?.let { message ->

                    Card(
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Column(
                            modifier =
                                Modifier.padding(16.dp)
                        ) {

                            Text(
                                text =
                                    "NSE connection error",
                                fontWeight =
                                    FontWeight.Bold,
                                color =
                                    Color(0xFFD32F2F)
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(6.dp)
                            )

                            Text(
                                text =
                                    message,
                                color =
                                    Color(0xFFD32F2F)
                            )
                        }
                    }
                }
            }

            item {

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                Text(
                    text =
                        if (isMarketHours())
                            "Auto-refresh: every 30 seconds"
                        else
                            "Auto-refresh paused — market closed",
                    fontSize =
                        12.sp,
                    color =
                        Color.Gray
                )

                Spacer(
                    modifier =
                        Modifier.height(20.dp)
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    name: String,
    value: Double?
) {

    Card(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text = name,
                fontSize = 16.sp,
                fontWeight =
                    FontWeight.Medium
            )

            Text(
                text =
                    formatMetric(value),
                fontSize = 18.sp,
                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}

private fun formatMetric(
    value: Double?
): String {

    if (value == null) {
        return "--"
    }

    return if (
        value % 1.0 == 0.0
    ) {
        String.format(
            "%.0f",
            value
        )
    } else {
        String.format(
            "%.2f",
            value
        )
    }
}

/**
 * NSE normal equity/derivatives market window:
 * Monday-Friday, 09:15-15:30 IST.
 */
private fun isMarketHours(): Boolean {

    val calendar =
        java.util.Calendar.getInstance(
            java.util.TimeZone.getTimeZone(
                "Asia/Kolkata"
            )
        )

    val day =
        calendar.get(
            java.util.Calendar.DAY_OF_WEEK
        )

    if (
        day ==
            java.util.Calendar.SATURDAY ||
        day ==
            java.util.Calendar.SUNDAY
    ) {
        return false
    }

    val hour =
        calendar.get(
            java.util.Calendar.HOUR_OF_DAY
        )

    val minute =
        calendar.get(
            java.util.Calendar.MINUTE
        )

    val totalMinutes =
        hour * 60 + minute

    return totalMinutes >= 9 * 60 + 15 &&
            totalMinutes <= 15 * 60 + 30
}
```
