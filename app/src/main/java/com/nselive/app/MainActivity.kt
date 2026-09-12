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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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


enum class Market {
    NSE,
    BSE,
    MCX
}


data class MarketSymbol(
    val displayName: String,
    val apiSymbol: String
)


private val NSE_SYMBOLS = listOf(
    MarketSymbol("NIFTY 50", "NIFTY"),
    MarketSymbol("BANKNIFTY", "BANKNIFTY"),
    MarketSymbol("NIFTYNXT50", "NIFTYNXT50"),
    MarketSymbol("FINNIFTY", "FINNIFTY")
)


private val BSE_SYMBOLS = listOf(
    MarketSymbol("SENSEX", "SENSEX"),
    MarketSymbol("BANKEX", "BANKEX"),
    MarketSymbol("BSE FOCUSED", "BSEFOCUSEDIT")
)


private val MCX_SYMBOLS = listOf(
    MarketSymbol("CRUDEOIL", "CRUDEOIL"),
    MarketSymbol("NATURALGAS", "NATURALGAS"),
    MarketSymbol("SILVER", "SILVER"),
    MarketSymbol("GOLD", "GOLD"),
    MarketSymbol("GOLDM", "GOLDM"),
    MarketSymbol("SILVERM", "SILVERM")
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

    var selectedMarket by remember {
        mutableStateOf(Market.NSE)
    }

    var selectedSymbol by remember {
        mutableStateOf(NSE_SYMBOLS.first())
    }

    var selectedExpiry by remember {
        mutableStateOf("")
    }

    var expiryDates by remember {
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

    val symbols = when (selectedMarket) {
        Market.NSE -> NSE_SYMBOLS
        Market.BSE -> BSE_SYMBOLS
        Market.MCX -> MCX_SYMBOLS
    }


    /*
     * When the exchange changes, select the first symbol
     * from that exchange and clear the expiry/data.
     */
    fun changeMarket(market: Market) {

        selectedMarket = market

        val newSymbols = when (market) {
            Market.NSE -> NSE_SYMBOLS
            Market.BSE -> BSE_SYMBOLS
            Market.MCX -> MCX_SYMBOLS
        }

        selectedSymbol = newSymbols.first()

        selectedExpiry = ""

        expiryDates = emptyList()

        metrics = NseMetrics()
    }


    /*
     * Load NSE expiry dates whenever the NSE symbol changes.
     */
    LaunchedEffect(
        selectedMarket,
        selectedSymbol.apiSymbol
    ) {

        if (selectedMarket != Market.NSE) {

            expiryDates = emptyList()
            selectedExpiry = ""

            metrics = NseMetrics(
                error = when (selectedMarket) {
                    Market.BSE ->
                        "BSE data connection will be added next."

                    Market.MCX ->
                        "MCX data connection will be added next."

                    else -> null
                }
            )

            return@LaunchedEffect
        }


        val api = NseApi()

        loading = true

        try {

            val expiries =
                api.getExpiries(
                    selectedSymbol.apiSymbol
                )

            expiryDates = expiries

            if (expiries.isNotEmpty()) {

                selectedExpiry = expiries.first()
            }

        } catch (e: Exception) {

            expiryDates = emptyList()

            selectedExpiry = ""

            metrics = metrics.copy(
                error = e.message
                    ?: "Unable to load NSE expiry dates"
            )

        } finally {

            loading = false
        }
    }


    /*
     * Load option-chain data whenever:
     *
     * Exchange
     * Symbol
     * Expiry
     *
     * changes.
     *
     * Also refresh every 30 seconds.
     */
    LaunchedEffect(
        selectedMarket,
        selectedSymbol.apiSymbol,
        selectedExpiry
    ) {

        if (
            selectedMarket != Market.NSE ||
            selectedExpiry.isBlank()
        ) {
            return@LaunchedEffect
        }


        val api = NseApi()


        while (true) {

            loading = true


            try {

                val optionChain =
                    api.getOptionChain(
                        symbol = selectedSymbol.apiSymbol,
                        expiry = selectedExpiry
                    )


                val vix =
                    try {

                        api.getIndiaVix()

                    } catch (e: Exception) {

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
                            ?: "Failed to load NSE data"
                    )

            } finally {

                loading = false
            }


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
                            fontWeight = FontWeight.Bold
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
             * NSE LOGO
             */
            item {

                Image(

                    painter =
                        painterResource(
                            id = R.drawable.nse_logo
                        ),

                    contentDescription = "NSE Live",

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(90.dp),

                    contentScale =
                        ContentScale.Fit
                )
            }


            /*
             * MARKET SELECTION
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
                            text = "Market",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )


                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
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
                                    selectedMarket ==
                                            Market.NSE,

                                onClick = {
                                    changeMarket(
                                        Market.NSE
                                    )
                                }
                            )


                            MarketRadioButton(
                                text = "BSE",
                                selected =
                                    selectedMarket ==
                                            Market.BSE,

                                onClick = {
                                    changeMarket(
                                        Market.BSE
                                    )
                                }
                            )


                            MarketRadioButton(
                                text = "MCX",
                                selected =
                                    selectedMarket ==
                                            Market.MCX,

                                onClick = {
                                    changeMarket(
                                        Market.MCX
                                    )
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

                Card(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Column(
                        modifier =
                            Modifier.padding(16.dp)
                    ) {

                        Text(
                            text = "Symbol",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )


                        Spacer(
                            modifier =
                                Modifier.height(6.dp)
                        )


                        ExposedDropdownMenuBox(

                            expanded =
                                symbolMenuExpanded,

                            onExpandedChange = {
                                symbolMenuExpanded =
                                    !symbolMenuExpanded
                            }

                        ) {

                            Button(

                                onClick = {
                                    symbolMenuExpanded =
                                        !symbolMenuExpanded
                                },

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
                                        text =
                                            selectedSymbol.displayName
                                    )

                                    Text("▼")
                                }
                            }


                            DropdownMenu(

                                expanded =
                                    symbolMenuExpanded,

                                onDismissRequest = {
                                    symbolMenuExpanded =
                                        false
                                }

                            ) {

                                symbols.forEach { symbol ->

                                    DropdownMenuItem(

                                        text = {

                                            Text(
                                                text =
                                                    symbol.displayName
                                            )
                                        },

                                        onClick = {

                                            selectedSymbol =
                                                symbol

                                            selectedExpiry =
                                                ""

                                            expiryDates =
                                                emptyList()

                                            symbolMenuExpanded =
                                                false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }


            /*
             * EXPIRY DROPDOWN
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
                            text = "Expiry Date",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )


                        Spacer(
                            modifier =
                                Modifier.height(6.dp)
                        )


                        Button(

                            enabled =
                                expiryDates.isNotEmpty(),

                            onClick = {

                                expiryMenuExpanded =
                                    !expiryMenuExpanded
                            },

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

                                    text =
                                        if (
                                            selectedExpiry.isBlank()
                                        ) {
                                            if (
                                                selectedMarket ==
                                                    Market.NSE
                                            ) {
                                                "Loading expiry..."
                                            } else {
                                                "Not available"
                                            }
                                        } else {
                                            selectedExpiry
                                        }
                                )

                                Text("▼")
                            }
                        }


                        DropdownMenu(

                            expanded =
                                expiryMenuExpanded,

                            onDismissRequest = {
                                expiryMenuExpanded =
                                    false
                            }

                        ) {

                            expiryDates.forEach { expiry ->

                                DropdownMenuItem(

                                    text = {

                                        Text(
                                            text = expiry
                                        )
                                    },

                                    onClick = {

                                        selectedExpiry =
                                            expiry

                                        expiryMenuExpanded =
                                            false
                                    }
                                )
                            }
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
                                Color(0xFFD32F2F)
                        )
                    }
                }
            }


            /*
             * REFRESH INFORMATION
             */
            item {

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )


                Text(
                    text =
                        when (selectedMarket) {

                            Market.NSE ->
                                "NSE data auto-refreshes every 30 seconds"

                            Market.BSE ->
                                "BSE API connection pending"

                            Market.MCX ->
                                "MCX API connection pending"
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


/*
 * MARKET RADIO BUTTON
 */
@Composable
private fun MarketRadioButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    Row(

        verticalAlignment =
            Alignment.CenterVertically

    ) {

        RadioButton(

            selected = selected,

            onClick = onClick
        )


        Text(
            text = text,
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
 * METRIC CARD
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
 * FORMAT METRIC VALUE
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
