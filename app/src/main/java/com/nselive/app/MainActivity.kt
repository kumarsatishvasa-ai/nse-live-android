package com.nselive.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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

    /*
     * Create NSE API client once.
     */
    val api = remember {
        NseApi()
    }


    /*
     * Fetch NSE data.
     *
     * First:
     * 1. Get NIFTY expiry dates
     * 2. Select nearest expiry
     * 3. Download option chain
     * 4. Download India VIX
     * 5. Calculate metrics
     *
     * Then repeat every 30 seconds.
     */
    LaunchedEffect(Unit) {

        while (true) {

            loading = true

            try {

                /*
                 * Get available NIFTY expiries.
                 */
                val expiries =
                    api.getExpiries(
                        symbol = "NIFTY"
                    )


                /*
                 * Use the nearest available expiry.
                 */
                val expiry =
                    expiries.firstOrNull()
                        ?: throw NseApiException(
                            "No NIFTY expiry returned by NSE"
                        )


                /*
                 * Get NIFTY option chain.
                 */
                val optionChain =
                    api.getOptionChain(
                        symbol = "NIFTY",
                        expiry = expiry
                    )


                /*
                 * India VIX is independent of
                 * the option-chain request.
                 *
                 * If VIX fails, the rest of the
                 * option-chain metrics can still display.
                 */
                val vix =
                    try {

                        api.getIndiaVix()

                    } catch (e: Exception) {

                        null
                    }


                /*
                 * Calculate:
                 *
                 * PCR OI
                 * PCR Volume
                 * Max Pain
                 * Gamma Flip
                 * Call Wall
                 * Put Wall
                 * Expected Move
                 * India VIX
                 */
                metrics =
                    MetricsCalculator.calculate(
                        chain = optionChain,
                        vix = vix
                    ).copy(
                        error = null
                    )


            } catch (e: Exception) {

                /*
                 * Keep previously displayed values
                 * but show the actual error.
                 */
                metrics =
                    metrics.copy(
                        error =
                            e.message
                                ?: "Failed to load NSE data"
                    )

            }

            loading = false


            /*
             * Wait 30 seconds before the
             * next NSE request.
             */
            delay(30_000)
        }
    }


    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Column {

                        Text(
                            text =
                                "SATHISH KUMAR VASA Option Analytics",
                            fontWeight =
                                FontWeight.Bold
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

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),

            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {


            /*
             * NSE Logo
             */
            item {

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                Image(

                    painter =
                        painterResource(
                            id = R.drawable.nse_logo
                        ),

                    contentDescription =
                        "SATHISH KUMAR VASA",

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(90.dp),

                    contentScale =
                        ContentScale.Fit
                )
            }


            /*
             * Loading indicator
             */
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

                        CircularProgressIndicator(

                            modifier =
                                Modifier
                                    .width(22.dp)
                                    .height(22.dp)
                        )

                        Spacer(
                            modifier =
                                Modifier.width(10.dp)
                        )

                        Text(
                            text =
                                "Loading NSE data..."
                        )
                    }
                }
            }


            /*
             * PCR OI
             */
            item {

                MetricCard(

                    name =
                        "PCR (OI)",

                    value =
                        formatMetric(
                            metrics.pcrOi
                        )
                )
            }


            /*
             * PCR Volume
             */
            item {

                MetricCard(

                    name =
                        "PCR (Volume)",

                    value =
                        formatMetric(
                            metrics.pcrVolume
                        )
                )
            }


            /*
             * Max Pain
             */
            item {

                MetricCard(

                    name =
                        "Max Pain",

                    value =
                        formatMetric(
                            metrics.maxPain
                        )
                )
            }


            /*
             * Gamma Flip
             */
            item {

                MetricCard(

                    name =
                        "Gamma Flip",

                    value =
                        formatMetric(
                            metrics.gammaFlip
                        )
                )
            }


            /*
             * Call Wall
             */
            item {

                MetricCard(

                    name =
                        "Call Wall",

                    value =
                        formatMetric(
                            metrics.callWall
                        )
                )
            }


            /*
             * Put Wall
             */
            item {

                MetricCard(

                    name =
                        "Put Wall",

                    value =
                        formatMetric(
                            metrics.putWall
                        )
                )
            }


            /*
             * Expected Move
             */
            item {

                MetricCard(

                    name =
                        "Expected Move",

                    value =
                        formatMetric(
                            metrics.expectedMove
                        )
                )
            }


            /*
             * India VIX
             */
            item {

                MetricCard(

                    name =
                        "India VIX",

                    value =
                        formatMetric(
                            metrics.indiaVix
                        )
                )
            }


            /*
             * Last Updated
             */
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

                            fontSize =
                                13.sp,

                            color =
                                Color.Gray
                        )

                        Spacer(
                            modifier =
                                Modifier.height(4.dp)
                        )

                        Text(

                            text =
                                metrics.updatedAt,

                            fontSize =
                                18.sp,

                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }
            }


            /*
             * Error message
             */
            item {

                metrics.error?.let { error ->

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
                                    "NSE Connection Error",

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
                                    error,

                                color =
                                    Color(0xFFD32F2F)
                            )
                        }
                    }
                }
            }


            /*
             * Refresh information
             */
            item {

                Spacer(
                    modifier =
                        Modifier.height(20.dp)
                )

                Text(

                    text =
                        "Auto-refresh: every 30 seconds",

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


/*
 * Metric card
 */
@Composable
private fun MetricCard(
    name: String,
    value: String
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

                text =
                    name,

                fontSize =
                    16.sp,

                fontWeight =
                    FontWeight.Medium
            )

            Text(

                text =
                    value,

                fontSize =
                    18.sp,

                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}


/*
 * Format numbers for display.
 */
private fun formatMetric(
    value: Double?
): String {

    return value?.let {

        if (it % 1.0 == 0.0) {

            String.format(
                "%.0f",
                it
            )

        } else {

            String.format(
                "%.2f",
                it
            )
        }

    } ?: "--"
}
