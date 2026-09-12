package com.nselive.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
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
                MarketLiveScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketLiveScreen() {

    /*
     * ---------------------------------------------------------
     * MARKET SELECTION
     * ---------------------------------------------------------
     */

    var selectedMarket by remember {
        mutableStateOf("NSE")
    }

    /*
     * ---------------------------------------------------------
     * SYMBOL SELECTION
     * ---------------------------------------------------------
     */

    var selectedSymbol by remember {
        mutableStateOf("NIFTY")
    }

    /*
     * ---------------------------------------------------------
     * EXPIRY SELECTION
     * ---------------------------------------------------------
     */

    var selectedExpiry by remember {
        mutableStateOf("")
    }

    /*
     * ---------------------------------------------------------
     * DROPDOWN STATES
     * ---------------------------------------------------------
     */

    var marketMenuExpanded by remember {
        mutableStateOf(false)
    }

    var symbolMenuExpanded by remember {
        mutableStateOf(false)
    }

    var expiryMenuExpanded by remember {
        mutableStateOf(false)
    }

    /*
     * ---------------------------------------------------------
     * NSE SYMBOLS
     * ---------------------------------------------------------
     */

    val nseSymbols = listOf(
        "NIFTY",
        "BANKNIFTY",
        "NIFTYNXT50",
        "FINNIFTY"
    )

    /*
     * ---------------------------------------------------------
     * MCX SYMBOLS
     * ---------------------------------------------------------
     */

    val mcxSymbols = listOf(
        "CRUDEOIL",
        "NATURALGAS",
        "SILVER",
        "GOLD",
        "GOLDM",
        "SILVERM"
    )

    /*
     * ---------------------------------------------------------
     * BSE SYMBOLS
     * ---------------------------------------------------------
     */

    val bseSymbols = listOf(
        "SENSEX",
        "BANKEX",
        "BSEFOCUSED"
    )

    /*
     * ---------------------------------------------------------
     * SELECT SYMBOL LIST
     * ---------------------------------------------------------
     */

    val availableSymbols =
        when (selectedMarket) {

            "NSE" -> nseSymbols

            "BSE" -> bseSymbols

            "MCX" -> mcxSymbols

            else -> nseSymbols
        }

    /*
     * ---------------------------------------------------------
     * RESET SYMBOL WHEN MARKET CHANGES
     * ---------------------------------------------------------
     */

    LaunchedEffect(selectedMarket) {

        selectedSymbol =
            availableSymbols.first()

        selectedExpiry = ""
    }

    /*
     * ---------------------------------------------------------
     * METRICS
     * ---------------------------------------------------------
     */

    var metrics by remember {
        mutableStateOf(NseMetrics())
    }

    var loading by remember {
        mutableStateOf(false)
    }

    /*
     * ---------------------------------------------------------
     * LOAD NSE DATA
     * ---------------------------------------------------------
     *
     * Currently the NseApi is used for NSE.
     *
     * MCX/BSE can be connected to their APIs separately.
     */

    LaunchedEffect(
        selectedMarket,
        selectedSymbol,
        selectedExpiry
    ) {

        if (selectedMarket != "NSE") {
            return@LaunchedEffect
        }

        val api = NseApi()

        while (true) {

            loading = true

            try {

                val expiries =
                    api.getExpiries(
                        selectedSymbol
                    )

                if (expiries.isNotEmpty()) {

                    if (
                        selectedExpiry.isEmpty() ||
                        selectedExpiry !in expiries
                    ) {
                        selectedExpiry =
                            expiries.first()
                    }

                    val expiry =
                        if (selectedExpiry.isNotEmpty()) {
                            selectedExpiry
                        } else {
                            expiries.first()
                        }

                    val optionChain =
                        api.getOptionChain(
                            symbol = selectedSymbol,
                            expiry = expiry
                        )

                    val vix =
                        try {
                            api.getIndiaVix()
                        } catch (
                            e: Exception
                        ) {
                            null
                        }

                    metrics =
                        MetricsCalculator.calculate(
                            chain = optionChain,
                            vix = vix
                        )
                }

            } catch (
                e: Exception
            ) {

                metrics =
                    metrics.copy(
                        error =
                            e.message
                                ?: "Failed to load NSE data"
                    )
            }

            loading = false

            delay(30_000)
        }
    }

    /*
     * ---------------------------------------------------------
     * SCREEN
     * ---------------------------------------------------------
     */

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Column {

                        Text(
                            text =
                                "SATHISH KUMAR VASA",
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            text =
                                "Option Analytics",
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
             * -------------------------------------------------
             * LOGO
             * -------------------------------------------------
             */

            item {

                Image(

                    painter =
                        painterResource(
                            id = R.drawable.nse_logo
                        ),

                    contentDescription =
                        "NSE Live",

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(90.dp),

                    contentScale =
                        ContentScale.Fit
                )
            }

            /*
             * -------------------------------------------------
             * MARKET RADIO BUTTONS
             * -------------------------------------------------
             */

            item {

                Text(
                    text = "Market",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.SpaceEvenly
                ) {

                    MarketRadioButton(
                        name = "NSE",
                        selected =
                            selectedMarket == "NSE"
                    ) {

                        selectedMarket = "NSE"

                        marketMenuExpanded =
                            false

                        symbolMenuExpanded =
                            false

                        expiryMenuExpanded =
                            false
                    }

                    MarketRadioButton(
                        name = "BSE",
                        selected =
                            selectedMarket == "BSE"
                    ) {

                        selectedMarket = "BSE"

                        marketMenuExpanded =
                            false

                        symbolMenuExpanded =
                            false

                        expiryMenuExpanded =
                            false
                    }

                    MarketRadioButton(
                        name = "MCX",
                        selected =
                            selectedMarket == "MCX"
                    ) {

                        selectedMarket = "MCX"

                        marketMenuExpanded =
                            false

                        symbolMenuExpanded =
                            false

                        expiryMenuExpanded =
                            false
                    )
                }
            }

            /*
             * -------------------------------------------------
             * MARKET DROPDOWN
             * -------------------------------------------------
             */

            item {

                SimpleDropdown(

                    label = "Market",

                    selectedValue =
                        selectedMarket,

                    expanded =
                        marketMenuExpanded,

                    onExpandedChange = {

                        marketMenuExpanded =
                            !marketMenuExpanded
                    },

                    options =
                        listOf(
                            "NSE",
                            "BSE",
                            "MCX"
                        ),

                    onSelected = {

                        selectedMarket = it

                        marketMenuExpanded =
                            false
                    }
                )
            }

            /*
             * -------------------------------------------------
             * SYMBOL DROPDOWN
             * -------------------------------------------------
             */

            item {

                SimpleDropdown(

                    label = "Symbol",

                    selectedValue =
                        selectedSymbol,

                    expanded =
                        symbolMenuExpanded,

                    onExpandedChange = {

                        symbolMenuExpanded =
                            !symbolMenuExpanded
                    },

                    options =
                        availableSymbols,

                    onSelected = {

                        selectedSymbol = it

                        selectedExpiry = ""

                        symbolMenuExpanded =
                            false
                    }
                )
            }

            /*
             * -------------------------------------------------
             * EXPIRY DROPDOWN
             * -------------------------------------------------
             */

            item {

                if (selectedMarket == "NSE") {

                    SimpleDropdown(

                        label = "Expiry",

                        selectedValue =
                            if (
                                selectedExpiry.isEmpty()
                            ) {
                                "Loading expiry..."
                            } else {
                                selectedExpiry
                            },

                        expanded =
                            expiryMenuExpanded,

                        onExpandedChange = {

                            expiryMenuExpanded =
                                !expiryMenuExpanded
                        },

                        options =
                            getExpiryOptions(
                                selectedExpiry
                            ),

                        onSelected = {

                            selectedExpiry = it

                            expiryMenuExpanded =
                                false
                        }
                    )
                }
            }

            /*
             * -------------------------------------------------
             * LOADING
             * -------------------------------------------------
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
             * -------------------------------------------------
             * NON-NSE MESSAGE
             * -------------------------------------------------
             */

            item {

                if (selectedMarket != "NSE") {

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
                                    "$selectedMarket selected"
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(5.dp)
                            )

                            Text(
                                text =
                                    "The $selectedMarket data connection will be added next.",
                                color =
                                    Color.Gray,
                                fontSize =
                                    13.sp
                            )
                        }
                    }
                }
            }

            /*
             * -------------------------------------------------
             * METRICS
             * -------------------------------------------------
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
             * -------------------------------------------------
             * LAST UPDATED
             * -------------------------------------------------
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
             * -------------------------------------------------
             * ERROR
             * -------------------------------------------------
             */

            item {

                metrics.error?.let { error ->

                    Card(
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Text(
                            text =
                                error,

                            modifier =
                                Modifier.padding(16.dp),

                            color =
                                Color(0xFFD32F2F)
                        )
                    }
                }
            }

            /*
             * -------------------------------------------------
             * REFRESH MESSAGE
             * -------------------------------------------------
             */

            item {

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
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
 * =============================================================
 * MARKET RADIO BUTTON
 * =============================================================
 */

@Composable
private fun MarketRadioButton(
    name: String,
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

            selected =
                selected,

            onClick =
                onClick
        )

        Text(
            text = name,
            fontWeight =
                if (selected) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                }
        )
    }
}

/*
 * =============================================================
 * NORMAL DROPDOWN
 *
 * IMPORTANT:
 * This does NOT use ExposedDropdownMenuBox.
 *
 * Therefore it avoids the FocusRequester crash.
 * =============================================================
 */

@Composable
private fun SimpleDropdown(
    label: String,
    selectedValue: String,
    expanded: Boolean,
    onExpandedChange: () -> Unit,
    options: List<String>,
    onSelected: (String) -> Unit
) {

    Column(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Text(
            text = label,

            fontSize =
                13.sp,

            color =
                Color.Gray
        )

        Spacer(
            modifier =
                Modifier.height(4.dp)
        )

        BoxDropdownButton(
            text =
                selectedValue,

            expanded =
                expanded,

            onClick =
                onExpandedChange
        )

        DropdownMenu(

            expanded =
                expanded,

            onDismissRequest = {
                onExpandedChange()
            },

            modifier =
                Modifier.fillMaxWidth(0.9f)
        ) {

            options.forEach { option ->

                DropdownMenuItem(

                    text = {

                        Text(
                            text = option
                        )
                    },

                    onClick = {

                        onSelected(option)
                    }
                )
            }
        }
    }
}

/*
 * =============================================================
 * DROPDOWN BUTTON
 * =============================================================
 */

@Composable
private fun BoxDropdownButton(
    text: String,
    expanded: Boolean,
    onClick: () -> Unit
) {

    Button(

        onClick =
            onClick,

        modifier =
            Modifier.fillMaxWidth()
    ) {

        Row(

            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.SpaceBetween,

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text = text
            )

            Text(
                text =
                    if (expanded) {
                        "▲"
                    } else {
                        "▼"
                    }
            )
        }
    }
}

/*
 * =============================================================
 * METRIC CARD
 * =============================================================
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

                fontSize =
                    16.sp,

                fontWeight =
                    FontWeight.Medium
            )

            Text(

                text = value,

                fontSize =
                    18.sp,

                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}

/*
 * =============================================================
 * EXPIRY OPTIONS
 * =============================================================
 *
 * The actual NSE expiry list is loaded by NseApi.
 *
 * The currently selected expiry is kept here so the dropdown
 * remains usable even before the first API response arrives.
 * =============================================================
 */

private fun getExpiryOptions(
    selectedExpiry: String
): List<String> {

    return if (selectedExpiry.isNotEmpty()) {

        listOf(selectedExpiry)

    } else {

        listOf(
            "Loading expiry..."
        )
    }
}

/*
 * =============================================================
 * NUMBER FORMAT
 * =============================================================
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
