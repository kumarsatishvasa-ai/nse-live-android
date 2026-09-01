```kotlin
package com.nselive.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
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

data class NseMetrics(
    val pcrOi: Double? = null,
    val pcrVolume: Double? = null,
    val maxPain: Double? = null,
    val gammaFlip: Double? = null,
    val callWall: Double? = null,
    val putWall: Double? = null,
    val expectedMove: Double? = null,
    val indiaVix: Double? = null,
    val updatedAt: String = "--:--:--",
    val error: String? = null
)

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NseLiveScreen() {

    var metrics by remember {
        mutableStateOf(NseMetrics())
    }

    var loading by remember {
        mutableStateOf(true)
    }

    LaunchedEffect(Unit) {
        while (true) {

            // Network/calculation layer will be connected in the next files.
            loading = true

            delay(1000)

            loading = false

            delay(29_000)
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
                            fontSize = 12.sp
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            item {
                Spacer(modifier = Modifier.height(8.dp))

                if (loading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.width(22.dp).height(22.dp)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Text("Loading NSE data...")
                    }
                }
            }

            item {
                MetricCard(
                    name = "PCR (OI)",
                    value = formatMetric(metrics.pcrOi)
                )
            }

            item {
                MetricCard(
                    name = "PCR (Volume)",
                    value = formatMetric(metrics.pcrVolume)
                )
            }

            item {
                MetricCard(
                    name = "Max Pain",
                    value = formatMetric(metrics.maxPain)
                )
            }

            item {
                MetricCard(
                    name = "Gamma Flip",
                    value = formatMetric(metrics.gammaFlip)
                )
            }

            item {
                MetricCard(
                    name = "Call Wall",
                    value = formatMetric(metrics.callWall)
                )
            }

            item {
                MetricCard(
                    name = "Put Wall",
                    value = formatMetric(metrics.putWall)
                )
            }

            item {
                MetricCard(
                    name = "Expected Move",
                    value = formatMetric(metrics.expectedMove)
                )
            }

            item {
                MetricCard(
                    name = "India VIX",
                    value = formatMetric(metrics.indiaVix)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Last Updated",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = metrics.updatedAt,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item {
                metrics.error?.let { error ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = error,
                            modifier = Modifier.padding(16.dp),
                            color = Color(0xFFD32F2F)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Auto-refresh: every 30 seconds",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun MetricCard(
    name: String,
    value: String
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatMetric(value: Double?): String {
    return value?.let {
        if (it % 1.0 == 0.0) {
            String.format("%.0f", it)
        } else {
            String.format("%.2f", it)
        }
    } ?: "--"
}
```
