package com.nselive.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale

class MainActivity : ComponentActivity() {

```
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
        MaterialTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                McxScreen()
            }
        }
    }
}
```

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun McxScreen(
mcxViewModel: McxViewModel = viewModel()
) {
val mcxState by mcxViewModel.uiState.collectAsState()

```
Scaffold(
    topBar = {
        TopAppBar(
            title = {
                Text(
                    text = "MCX Option Chain",
                    fontWeight = FontWeight.Bold
                )
            }
        )
    }
) { paddingValues ->

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(12.dp)
    ) {

        SymbolSelector(
            symbol = mcxState.symbol,
            onSymbolSelected = {
                mcxViewModel.setSymbol(it)
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        ExpirySelector(
            expiries = mcxState.expiries,
            selectedExpiry = mcxState.selectedExpiry,
            onExpirySelected = {
                mcxViewModel.selectExpiry(it)
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            OutlinedButton(
                onClick = {
                    mcxViewModel.refresh()
                }
            ) {
                Text("Refresh")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        mcxState.error?.let { error ->

            ErrorCard(
                message = error,
                onRetry = {
                    mcxViewModel.retry()
                },
                onDismiss = {
                    mcxViewModel.clearError()
                }
            )

            Spacer(modifier = Modifier.height(10.dp))
        }

        if (mcxState.loading) {

            LoadingView()

        } else {

            mcxState.chain?.let { chain ->

                ChainHeader(chain = chain)

                Spacer(modifier = Modifier.height(10.dp))
            }

            mcxState.metrics?.let { metrics ->

                MetricsCard(metrics = metrics)

                Spacer(modifier = Modifier.height(10.dp))
            }

            mcxState.chain?.let { chain ->

                OptionChainTable(chain = chain)
            }
        }
    }
}
```

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SymbolSelector(
symbol: String,
onSymbolSelected: (String) -> Unit
) {
val symbols = remember {
listOf(
"CRUDEOIL",
"GOLD",
"SILVER",
"NATURALGAS",
"COPPER",
"ZINC",
"LEAD",
"ALUMINIUM",
"NICKEL"
)
}

```
var expanded by remember {
    mutableStateOf(false)
}

ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = {
        expanded = !expanded
    },
    modifier = Modifier.fillMaxWidth()
) {

    OutlinedTextField(
        value = symbol,
        onValueChange = {},
        readOnly = true,
        label = {
            Text("MCX Symbol")
        },
        trailingIcon = {
            ExposedDropdownMenuDefaults.TrailingIcon(
                expanded = expanded
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .menuAnchor(),
        singleLine = true
    )

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = {
            expanded = false
        }
    ) {
        symbols.forEach { item ->

            DropdownMenuItem(
                text = {
                    Text(item)
                },
                onClick = {
                    expanded = false
                    onSymbolSelected(item)
                }
            )
        }
    }
}
```

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpirySelector(
expiries: List<String>,
selectedExpiry: String?,
onExpirySelected: (String) -> Unit
) {
var expanded by remember {
mutableStateOf(false)
}

```
if (expiries.isEmpty()) {

    OutlinedTextField(
        value = "",
        onValueChange = {},
        readOnly = true,
        label = {
            Text("Expiry")
        },
        placeholder = {
            Text("Loading expiries...")
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    return
}

ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = {
        expanded = !expanded
    },
    modifier = Modifier.fillMaxWidth()
) {

    OutlinedTextField(
        value = selectedExpiry ?: "",
        onValueChange = {},
        readOnly = true,
        label = {
            Text("Expiry")
        },
        trailingIcon = {
            ExposedDropdownMenuDefaults.TrailingIcon(
                expanded = expanded
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .menuAnchor(),
        singleLine = true
    )

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = {
            expanded = false
        }
    ) {

        expiries.forEach { expiry ->

            DropdownMenuItem(
                text = {
                    Text(expiry)
                },
                onClick = {
                    expanded = false
                    onExpirySelected(expiry)
                }
            )
        }
    }
}
```

}

@Composable
private fun LoadingView() {

```
Box(
    modifier = Modifier
        .fillMaxWidth()
        .padding(30.dp),
    contentAlignment = Alignment.Center
) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        CircularProgressIndicator()

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Loading MCX data..."
        )
    }
}
```

}

@Composable
private fun ErrorCard(
message: String,
onRetry: () -> Unit,
onDismiss: () -> Unit
) {
Card(
modifier = Modifier.fillMaxWidth(),
colors = CardDefaults.cardColors(
containerColor = MaterialTheme.colorScheme.errorContainer
)
) {

```
    Column(
        modifier = Modifier.padding(12.dp)
    ) {

        Text(
            text = "MCX Error",
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onErrorContainer
        )

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = message,
            color = MaterialTheme.colorScheme.onErrorContainer
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Button(
                onClick = onRetry
            ) {
                Text("Retry")
            }

            OutlinedButton(
                onClick = onDismiss
            ) {
                Text("Dismiss")
            }
        }
    }
}
```

}

@Composable
private fun ChainHeader(
chain: OptionChain
) {
Card(
modifier = Modifier.fillMaxWidth()
) {

```
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Column {

            Text(
                text = "Underlying",
                style = MaterialTheme.typography.labelMedium
            )

            Text(
                text = formatNumber(chain.underlyingValue),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }

        Column(
            horizontalAlignment = Alignment.End
        ) {

            Text(
                text = "Timestamp",
                style = MaterialTheme.typography.labelMedium
            )

            Text(
                text = chain.timestamp,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
```

}

@Composable
private fun MetricsCard(
metrics: NseMetrics
) {
Card(
modifier = Modifier.fillMaxWidth()
) {

```
    Column(
        modifier = Modifier.padding(12.dp)
    ) {

        Text(
            text = "MCX Analytics",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        MetricRow(
            label = "PCR OI",
            value = formatNullable(metrics.pcrOi)
        )

        MetricRow(
            label = "PCR Volume",
            value = formatNullable(metrics.pcrVolume)
        )

        MetricRow(
            label = "Max Pain",
            value = formatNullable(metrics.maxPain)
        )

        MetricRow(
            label = "Gamma Flip",
            value = formatNullable(metrics.gammaFlip)
        )

        MetricRow(
            label = "Call Wall",
            value = formatNullable(metrics.callWall)
        )

        MetricRow(
            label = "Put Wall",
            value = formatNullable(metrics.putWall)
        )

        MetricRow(
            label = "Expected Move",
            value = formatNullable(metrics.expectedMove)
        )

        MetricRow(
            label = "India VIX",
            value = formatNullable(metrics.indiaVix)
        )

        MetricRow(
            label = "Updated",
            value = metrics.updatedAt
        )
    }
}
```

}

@Composable
private fun MetricRow(
label: String,
value: String
) {
Row(
modifier = Modifier
.fillMaxWidth()
.padding(vertical = 4.dp),
horizontalArrangement = Arrangement.SpaceBetween
) {

```
    Text(
        text = label,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Text(
        text = value,
        fontWeight = FontWeight.SemiBold
    )
}
```

}

@Composable
private fun OptionChainTable(
chain: OptionChain
) {

```
if (chain.contracts.isEmpty()) {

    Text(
        text = "No option-chain contracts available.",
        modifier = Modifier.padding(16.dp)
    )

    return
}

val horizontalScrollState = rememberScrollState()

Card(
    modifier = Modifier.fillMaxWidth()
) {

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {

        Text(
            text = "Option Chain",
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(horizontalScrollState)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            TableHeader("Call OI", 90.dp)
            TableHeader("Call Vol", 90.dp)
            TableHeader("Call LTP", 90.dp)
            TableHeader("Strike", 100.dp)
            TableHeader("Put LTP", 90.dp)
            TableHeader("Put Vol", 90.dp)
            TableHeader("Put OI", 90.dp)
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(450.dp)
        ) {

            items(
                items = chain.contracts,
                key = {
                    it.strikePrice
                }
            ) { contract ->

                OptionRow(
                    contract = contract,
                    horizontalScrollState = horizontalScrollState
                )
            }
        }
    }
}
```

}

@Composable
private fun TableHeader(
text: String,
width: Dp
) {
Text(
text = text,
modifier = Modifier.width(width),
fontWeight = FontWeight.Bold,
style = MaterialTheme.typography.labelMedium
)
}

@Composable
private fun OptionRow(
contract: OptionContract,
horizontalScrollState: androidx.compose.foundation.ScrollState
) {
Row(
modifier = Modifier
.fillMaxWidth()
.horizontalScroll(horizontalScrollState)
.padding(vertical = 8.dp),
verticalAlignment = Alignment.CenterVertically
) {

```
    TableCell(
        text = formatNumber(contract.callOi),
        width = 90.dp
    )

    TableCell(
        text = formatNumber(contract.callVolume),
        width = 90.dp
    )

    TableCell(
        text = formatNumber(contract.callLtp),
        width = 90.dp
    )

    TableCell(
        text = formatNumber(contract.strikePrice),
        width = 100.dp,
        bold = true
    )

    TableCell(
        text = formatNumber(contract.putLtp),
        width = 90.dp
    )

    TableCell(
        text = formatNumber(contract.putVolume),
        width = 90.dp
    )

    TableCell(
        text = formatNumber(contract.putOi),
        width = 90.dp
    )
}
```

}

@Composable
private fun TableCell(
text: String,
width: Dp,
bold: Boolean = false
) {
Text(
text = text,
modifier = Modifier.width(width),
fontWeight = if (bold) {
FontWeight.Bold
} else {
FontWeight.Normal
},
style = MaterialTheme.typography.bodySmall
)
}

private fun formatNumber(
value: Double
): String {
return String.format(
Locale.US,
"%.2f",
value
)
}

private fun formatNullable(
value: Double?
): String {
return value?.let {
formatNumber(it)
} ?: "—"
}
