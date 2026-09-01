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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

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

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun NseLiveScreen(
    viewModel: NseViewModel = viewModel()
) {

    val state by
        viewModel.uiState.collectAsStateWithLifecycle()

    val pullRefreshState =
        rememberPullRefreshState(
            refreshing = state.refreshing,
            onRefresh = {
                viewModel.manualRefresh()
            }
        )

    Scaffold(
        topBar = {

            TopAppBar(
                title = {

                    Column {

                        Text(
                            text = "NSE LIVE",
                            fontWeight =
                                FontWeight.Bold
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

        val metrics =
            state.metrics

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .pullRefresh(
                        pullRefreshState
                    )
        ) {

            PullRefreshIndicator(
                refreshing = state.refreshing,
                state = pullRefreshState,
                modifier =
                    Modifier.align(
                        Alignment.CenterHorizontally
                    )
            )

            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = 16.dp
                        ),
                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                item {

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
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
                                if (
                                    NseViewModel
                                        .isMarketHours()
                                ) {
                                    "● MARKET OPEN"
                                } else {
                                    "● MARKET CLOSED"
                                },
                            color =
                                if (
                                    NseViewModel
                                        .isMarketHours()
                                ) {
                                    Color(0xFF2E7D32)
                                } else {
                                    Color.Gray
                                },
                            fontWeight =
                                FontWeight.Bold
                        )

                        Button(
                            enabled =
                                !state.loading,
                            onClick = {
                                viewModel.manualRefresh()
                            }
                        ) {
                            Text("Refresh")
                        }
                    }
                }

                item {

                    if (
                        state.loading &&
                        metrics == null
                    ) {

                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.Center,
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            CircularProgressIndicator(
                                modifier =
                                    Modifier
                                        .height(24.dp)
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(8.dp)
                            )

                            Text(
                                "Connecting to NSE..."
                            )
                        }
                    }
                }

                item {
                    MetricCard(
                        name = "PCR (OI)",
                        value = metrics?.pcrOi
                    )
                }

                item {
                    MetricCard(
                        name = "PCR (Volume)",
                        value =
                            metrics?.pcrVolume
                    )
                }

                item {
                    MetricCard(
                        name = "Max Pain",
                        value =
                            metrics?.maxPain
                    )
                }

                item {
                    MetricCard(
                        name = "Gamma Flip",
                        value =
                            metrics?.gammaFlip
                    )
                }

                item {
                    MetricCard(
                        name = "Call Wall",
                        value =
                            metrics?.callWall
                    )
                }

                item {
                    MetricCard(
                        name = "Put Wall",
                        value =
                            metrics?.putWall
                    )
                }

                item {
                    MetricCard(
                        name = "Expected Move",
                        value =
                            metrics?.expectedMove
                    )
                }

                item {
                    MetricCard(
                        name = "India VIX",
                        value =
                            metrics?.indiaVix
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
                                fontSize = 13.sp,
                                color = Color.Gray
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(4.dp)
                            )

                            Text(
                                text =
                                    metrics
                                        ?.updatedAt
                                        ?: "--:--:--",
                                fontSize = 18.sp,
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }
                    }
                }

                item {

                    state.error?.let { message ->

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
                                    text = message,
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
                            Modifier.height(8.dp)
                    )

                    Text(
                        text =
                            if (
                                NseViewModel
                                    .isMarketHours()
                            ) {
                                "Auto-refresh: every 30 seconds"
                            } else {
                                "Auto-refresh paused — market closed"
                            },
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    Spacer(
                        modifier =
                            Modifier.height(20.dp)
                    )
                }
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
```
