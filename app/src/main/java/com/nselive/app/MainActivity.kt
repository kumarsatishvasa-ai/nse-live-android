package com.nselive.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
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

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF5F5F5)
                ) {
                    MainScreen()
                }
            }
        }
    }
}

data class McxOptionRow(
    val strike: Double,
    val callOi: Double,
    val callOiChange: Double,
    val callVolume: Double,
    val callLtp: Double,
    val putOi: Double,
    val putOiChange: Double,
    val putVolume: Double,
    val putLtp: Double
)

data class McxUiState(
    val symbol: String = "CRUDEOIL",
    val underlyingValue: Double = 0.0,
    val expiry: String = "",
    val timestamp: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val data: List<McxOptionRow> = emptyList(),

    // Analytics
    val pcr: Double = 0.0,
    val maxPain: Double = 0.0,
    val gammaFlip: Double = 0.0,
    val callWall: Double = 0.0,
    val putWall: Double = 0.0,
    val expectedMove: Double = 0.0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {

    var selectedTab by remember {
        mutableStateOf("MCX")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SATHISH KUMAR VASA",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                TabButton(
                    title = "NSE",
                    selected = selectedTab == "NSE",
                    modifier = Modifier.weight(1f)
                ) {
                    selectedTab = "NSE"
                }

                TabButton(
                    title = "BSE",
                    selected = selectedTab == "BSE",
                    modifier = Modifier.weight(1f)
                ) {
                    selectedTab = "BSE"
                }

                TabButton(
                    title = "MCX",
                    selected = selectedTab == "MCX",
                    modifier = Modifier.weight(1f)
                ) {
                    selectedTab = "MCX"
                }
            }

            when (selectedTab) {
                "NSE" -> {
                    NseScreen()
                }

                "BSE" -> {
                    BseScreen()
                }

                "MCX" -> {
                    McxScreen()
                }
            }
        }
    }
}

@Composable
fun TabButton(
    title: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                Color(0xFF1565C0)
            } else {
                Color.White
            }
        ),
        onClick = onClick
    ) {
        Text(
            text = title,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = if (selected) Color.White else Color.Black,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun McxScreen() {

    /*
     * Temporary state.
     *
     * This is intentionally local so MainActivity.kt does not depend on
     * McxViewModel until the MCX API client is connected.
     *
     * Later this can be replaced with:
     *
     * val mcxViewModel = remember { McxViewModel() }
     * val mcxState by mcxViewModel.uiState.collectAsState()
     */

    val mcxState = remember {
        mutableStateOf(
            McxUiState(
                symbol = "CRUDEOIL"
            )
        )
    }

    val state = mcxState.value

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        item {
            Spacer(modifier = Modifier.height(4.dp))

            HeaderCard(state)
        }

        item {
            AnalyticsCard(state)
        }

        item {
            Text(
                text = "MCX OPTION CHAIN",
                modifier = Modifier.padding(
                    top = 8.dp,
                    bottom = 2.dp
                ),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        if (state.data.isEmpty()) {

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Text(
                            text = "MCX data not connected",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        Text(
                            text = "The UI is ready. Connect McxApi.kt to load real MCX option-chain data."
                        )
                    }
                }
            }

        } else {

            items(state.data) { option ->

                OptionRow(
                    option = option,
                    underlying = state.underlyingValue
                )
            }
        }
    }
}

@Composable
fun HeaderCard(state: McxUiState) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {

        Column(
            modifier = Modifier.padding(14.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Column {

                    Text(
                        text = state.symbol,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "MCX",
                        color = Color.Gray
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {

                    Text(
                        text = if (state.underlyingValue > 0) {
                            formatNumber(state.underlyingValue)
                        } else {
                            "--"
                        },
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1565C0)
                    )

                    Text(
                        text = "Underlying",
                        color = Color.Gray
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                InfoText(
                    label = "Expiry",
                    value = if (state.expiry.isBlank()) {
                        "--"
                    } else {
                        state.expiry
                    }
                )

                InfoText(
                    label = "Updated",
                    value = if (state.timestamp.isBlank()) {
                        "--"
                    } else {
                        state.timestamp
                    }
                )
            }

            if (state.error != null) {

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = state.error,
                    color = Color(0xFFC62828),
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun InfoText(
    label: String,
    value: String
) {

    Column {

        Text(
            text = label,
            color = Color.Gray,
            fontSize = 12.sp
        )

        Text(
            text = value,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp
        )
    }
}

@Composable
fun AnalyticsCard(state: McxUiState) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF101820)
        )
    ) {

        Column(
            modifier = Modifier.padding(14.dp)
        ) {

            Text(
                text = "MCX OPTIONS ANALYTICS",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            AnalyticsRow(
                "PCR",
                formatAnalytics(state.pcr)
            )

            AnalyticsRow(
                "Max Pain",
                formatAnalytics(state.maxPain)
            )

            AnalyticsRow(
                "Gamma Flip",
                formatAnalytics(state.gammaFlip)
            )

            AnalyticsRow(
                "Call Wall",
                formatAnalytics(state.callWall)
            )

            AnalyticsRow(
                "Put Wall",
                formatAnalytics(state.putWall)
            )

            AnalyticsRow(
                "Expected Move",
                formatAnalytics(state.expectedMove)
            )
        }
    }
}

@Composable
fun AnalyticsRow(
    title: String,
    value: String
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Text(
            text = title,
            color = Color(0xFFB0BEC5)
        )

        Text(
            text = value,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun OptionRow(
    option: McxOptionRow,
    underlying: Double
) {

    val isNearSpot =
        underlying > 0 &&
                kotlin.math.abs(option.strike - underlying) <
                underlying * 0.005

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isNearSpot) {
                Color(0xFFFFF3CD)
            } else {
                Color.White
            }
        )
    ) {

        Column(
            modifier = Modifier.padding(10.dp)
        ) {

            Text(
                text = "Strike ${formatNumber(option.strike)}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                OptionSide(
                    title = "CALL",
                    oi = option.callOi,
                    volume = option.callVolume,
                    ltp = option.callLtp,
                    oiChange = option.callOiChange,
                    color = Color(0xFF2E7D32)
                )

                OptionSide(
                    title = "PUT",
                    oi = option.putOi,
                    volume = option.putVolume,
                    ltp = option.putLtp,
                    oiChange = option.putOiChange,
                    color = Color(0xFFC62828)
                )
            }
        }
    }
}

@Composable
fun OptionSide(
    title: String,
    oi: Double,
    volume: Double,
    ltp: Double,
    oiChange: Double,
    color: Color
) {

    Column(
        modifier = Modifier
            .fillMaxWidth(0.5f)
            .padding(horizontal = 4.dp)
    ) {

        Text(
            text = title,
            color = color,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "OI: ${formatNumber(oi)}",
            fontSize = 13.sp
        )

        Text(
            text = "OI Chg: ${formatNumber(oiChange)}",
            fontSize = 13.sp
        )

        Text(
            text = "Volume: ${formatNumber(volume)}",
            fontSize = 13.sp
        )

        Text(
            text = "LTP: ${formatNumber(ltp)}",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun NseScreen() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "NSE",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Existing NSE screen"
        )
    }
}

@Composable
fun BseScreen() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "BSE",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "BSE option-chain UI ready"
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Connect BSE API to display live data."
        )
    }
}

fun formatNumber(value: Double): String {

    return if (value == 0.0) {
        "--"
    } else if (value % 1.0 == 0.0) {
        String.format(
            java.util.Locale.US,
            "%.0f",
            value
        )
    } else {
        String.format(
            java.util.Locale.US,
            "%.2f",
            value
        )
    }
}

fun formatAnalytics(value: Double): String {

    return if (value == 0.0) {
        "--"
    } else {
        String.format(
            java.util.Locale.US,
            "%.2f",
            value
        )
    }
}
