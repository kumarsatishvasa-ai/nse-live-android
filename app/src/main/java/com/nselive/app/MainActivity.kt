package com.nselive.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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

enum class MarketSegment {
    NSE,
    BSE,
    MCX
}

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

    var selectedSegment by remember {
        mutableStateOf(MarketSegment.NSE)
    }

    var selectedSymbol by remember {
        mutableStateOf("NIFTY")
    }

    var selectedExpiry by remember {
        mutableStateOf("")
    }

    var expiryOptions by remember {
        mutableStateOf<List<String>>(emptyList())
    }

    var metrics by remember {
        mutableStateOf(NseMetrics())
    }

    var loading by remember {
        mutableStateOf(false)
    }

    var symbolMenuExpanded by remember {
        mutableStateOf(false)
    }

    var expiryMenuExpanded by remember {
        mutableStateOf(false)
    }

    val api = remember {
        NseApi()
    }

    val symbols = when (selectedSegment) {

        MarketSegment.NSE -> listOf(
            "NIFTY",
            "BANKNIFTY",
            "NIFTYNXT50",
            "FINNIFTY"
        )

        MarketSegment.BSE -> listOf(
            "SENSEX",
            "BANKEX",
            "BSEFOCUSED"
        )

        MarketSegment.MCX -> listOf(
            "CRUDEOIL",
            "NATURALGAS",
            "SILVER",
            "GOLD",
            "GOLDM",
            "SILVERM"
        )
    }
val mcxViewModel =
    remember {
        McxViewModel()
    }

val mcxState by
    mcxViewModel.uiState.collectAsState()
    /*
     * Load expiry dates whenever the market segment or
     * symbol changes.
     *
     * Currently the NseApi supplied in the project supports
     * NSE option-chain expiry retrieval.
     */
    LaunchedEffect(selectedSegment, selectedSymbol) {

        expiryOptions = emptyList()
        selectedExpiry = ""

        if (selectedSegment == MarketSegment.NSE) {

            try {

                val expiries =
                    api.getExpiries(selectedSymbol)

                expiryOptions = expiries

                if (expiries.isNotEmpty()) {
                    selectedExpiry = expiries.first()
                }

            } catch (e: Exception) {

                metrics = metrics.copy(
                    error = e.message
                        ?: "Unable to load expiry dates"
                )
            }
        }
    }

    /*
     * Load live NSE data.
     *
     * BSE and MCX are displayed in the UI, but their
     * data endpoints are not being called here yet.
     */
    LaunchedEffect(
        selectedSegment,
        selectedSymbol,
        selectedExpiry
    ) {

        if (
            selectedSegment != MarketSegment.NSE ||
            selectedExpiry.isBlank()
        ) {
            return@LaunchedEffect
        }

        while (true) {

            loading = true

            try {

                val optionChain =
                    api.getOptionChain(
                        symbol = selectedSymbol,
                        expiry = selectedExpiry
                    )

                val vix =
                    try {
                        api.getIndiaVix()
                    } catch (_: Exception) {
                        null
                    }

                metrics =
                    MetricsCalculator.calculate(
                        chain = optionChain,
                        vix = vix
                    ).copy(
                        error = null
                    )

            } catch (e: Exception) {

                metrics =
                    metrics.copy(
                        error = e.message
                            ?: "Failed to load market data"
                    )
            }

            loading = false

            delay(30_000)
        }
    }

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Column {

                        Text(
                            text = "SATHISH KUMAR VASA",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )

                        Text(
                            text = "Option Analytics",
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

            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            /*
             * LOGO
             */
            item {

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
             * MARKET SEGMENT
             */
            item {

                Card(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Column(
                        modifier =
                            Modifier.padding(12.dp)
                    ) {

                        Text(
                            text = "Market",
                            fontWeight =
                                FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        Spacer(
                            modifier =
                                Modifier.height(6.dp)
                        )

                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.SpaceEvenly
                        ) {

                            MarketRadioButton(
                                text = "NSE",
                                selected =
                                    selectedSegment ==
                                            MarketSegment.NSE,
                                onClick = {

                                    selectedSegment =
                                        MarketSegment.NSE

                                    selectedSymbol =
                                        "NIFTY"

                                    metrics =
                                        NseMetrics()
                                }
                            )

                            MarketRadioButton(
                                text = "BSE",
                                selected =
                                    selectedSegment ==
                                            MarketSegment.BSE,
                                onClick = {

                                    selectedSegment =
                                        MarketSegment.BSE

                                    selectedSymbol =
                                        "SENSEX"

                                    selectedExpiry =
                                        ""

                                    expiryOptions =
                                        emptyList()

                                    metrics =
                                        NseMetrics()
                                }
                            )

                            MarketRadioButton(
                                text = "MCX",
                                selected =
                                    selectedSegment ==
                                            MarketSegment.MCX,
                                onClick = {

                                    selectedSegment =
                                        MarketSegment.MCX

                                    selectedSymbol =
                                        "CRUDEOIL"

                                    selectedExpiry =
                                        ""

                                    expiryOptions =
                                        emptyList()

                                    metrics =
                                        NseMetrics()
                                }
                            )
                        }
                    }
                }
            }

            /*
             * SYMBOL DROPDOWN
             */
            item {

                SimpleDropdown(

                    label = "Symbol",

                    selectedValue =
                        selectedSymbol,

                    options =
                        symbols,

                    expanded =
                        symbolMenuExpanded,

                    onExpandedChange = {
                        symbolMenuExpanded = it
                    },

                    onSelected = { symbol ->

                        selectedSymbol = symbol

                        symbolMenuExpanded = false

                        metrics =
                            NseMetrics()
                    }
                )
            }

            /*
             * EXPIRY DROPDOWN
             */
            item {

                if (
                    selectedSegment ==
                            MarketSegment.NSE
                ) {

                    SimpleDropdown(

                        label = "Expiry",

                        selectedValue =
                            selectedExpiry.ifBlank {
                                "Select expiry"
                            },

                        options =
                            expiryOptions,

                        expanded =
                            expiryMenuExpanded,

                        onExpandedChange = {
                            expiryMenuExpanded = it
                        },

                        onSelected = { expiry ->

                            selectedExpiry =
                                expiry

                            expiryMenuExpanded =
                                false
                        }
                    )

                } else {

                    Card(
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Column(
                            modifier =
                                Modifier.padding(16.dp)
                        ) {

                            Text(
                                text = "Expiry",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(4.dp)
                            )

                            Text(
                                text =
                                    "Expiry data will be connected for ${
                                        selectedSegment.name
                                    }",
                                fontWeight =
                                    FontWeight.Medium
                            )
                        }
                    }
                }
            }

            /*
             * LOADING
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
                                "Loading market data..."
                        )
                    }
                }
            }

            /*
             * ERROR
             */
            item {

                metrics.error?.let { error ->

                    Card(
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Text(
                            text = error,

                            modifier =
                                Modifier.padding(16.dp),

                            color =
                                Color(0xFFD32F2F),

                            fontWeight =
                                FontWeight.Medium
                        )
                    }
                }
            }

            /*
             * METRICS
             */
            item {

                MetricCard(
                    name = "PCR (OI)",
                    value =
                        formatMetric(
                            metrics.pcrOi
                        )
                )
            }

            item {

                MetricCard(
                    name = "PCR (Volume)",
                    value =
                        formatMetric(
                            metrics.pcrVolume
                        )
                )
            }

            item {

                MetricCard(
                    name = "Max Pain",
                    value =
                        formatMetric(
                            metrics.maxPain
                        )
                )
            }

            item {

                MetricCard(
                    name = "Gamma Flip",
                    value =
                        formatMetric(
                            metrics.gammaFlip
                        )
                )
            }

            item {

                MetricCard(
                    name = "Call Wall",
                    value =
                        formatMetric(
                            metrics.callWall
                        )
                )
            }

            item {

                MetricCard(
                    name = "Put Wall",
                    value =
                        formatMetric(
                            metrics.putWall
                        )
                )
            }

            item {

                MetricCard(
                    name = "Expected Move",
                    value =
                        formatMetric(
                            metrics.expectedMove
                        )
                )
            }

            item {

                MetricCard(
                    name = "India VIX",
                    value =
                        formatMetric(
                            metrics.indiaVix
                        )
                )
            }

            /*
             * LAST UPDATED
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
                            text = "Last Updated",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )

                        Spacer(
                            modifier =
                                Modifier.height(4.dp)
                        )

                        Text(
                            text =
                                metrics.updatedAt,

                            fontSize = 18.sp,

                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }
            }

            /*
             * REFRESH STATUS
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
                                "Live Market Connection",

                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            modifier =
                                Modifier.height(4.dp)
                        )

                        Text(
                            text =
                                when {

                                    selectedSegment ==
                                            MarketSegment.NSE &&
                                            !loading &&
                                            metrics.error == null ->
                                        "NSE data connected"

                                    selectedSegment !=
                                            MarketSegment.NSE ->
                                        "${selectedSegment.name} UI ready — data API not connected yet"

                                    loading ->
                                        "Connecting to NSE..."

                                    else ->
                                        "NSE connection error"
                                },

                            fontSize = 13.sp,

                            color =
                                if (
                                    selectedSegment ==
                                        MarketSegment.NSE &&
                                    metrics.error == null &&
                                    !loading
                                ) {
                                    Color(0xFF2E7D32)
                                } else {
                                    Color.Gray
                                }
                        )
                    }
                }
            }

            item {

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                Text(
                    text =
                        "Auto-refresh: every 30 seconds",

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

/*
 * Radio button used for NSE / BSE / MCX.
 */
@Composable
private fun MarketRadioButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    Row(

        modifier =
            Modifier.clickable {
                onClick()
            },

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        RadioButton(

            selected = selected,

            onClick = onClick
        )

        Text(
            text = text,
            fontSize = 14.sp
        )
    }
}

/*
 * Safe dropdown implementation.
 *
 * IMPORTANT:
 * This intentionally does NOT use ExposedDropdownMenuBox.
 * Therefore it avoids the FocusRequester crash you saw.
 */
@Composable
private fun SimpleDropdown(
    label: String,
    selectedValue: String,
    options: List<String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelected: (String) -> Unit
) {

    Card(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Box(
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Column(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable {

                            if (options.isNotEmpty()) {
                                onExpandedChange(!expanded)
                            }
                        }
                        .padding(16.dp)
            ) {

                Text(
                    text = label,
                    fontSize = 13.sp,
                    color = Color.Gray
                )

                Spacer(
                    modifier =
                        Modifier.height(5.dp)
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
                        text = selectedValue,
                        fontSize = 17.sp,
                        fontWeight =
                            FontWeight.Medium
                    )

                    Text(
                        text =
                            if (expanded) {
                                "▲"
                            } else {
                                "▼"
                            },

                        fontSize = 14.sp
                    )
                }
            }

            DropdownMenu(

                expanded = expanded,

                onDismissRequest = {
                    onExpandedChange(false)
                }
            ) {

                if (options.isEmpty()) {

                    DropdownMenuItem(

                        text = {
                            Text(
                                "No options available"
                            )
                        },

                        onClick = {
                            onExpandedChange(false)
                        }
                    )

                } else {

                    options.forEach { option ->

                        DropdownMenuItem(

                            text = {
                                Text(option)
                            },

                            onClick = {

                                onSelected(option)

                                onExpandedChange(false)
                            }
                        )
                    }
                }
            }
        }
    }
}

/*
 * Metric card.
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

                text = name,

                fontSize = 16.sp,

                fontWeight =
                    FontWeight.Medium
            )

            Text(

                text = value,

                fontSize = 18.sp,

                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}

/*
 * Number formatting.
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
