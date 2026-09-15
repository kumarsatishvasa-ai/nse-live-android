package com.nselive.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class McxUiState(
val symbol: String = "CRUDEOIL",
val expiries: List<String> = emptyList(),
val selectedExpiry: String? = null,
val chain: OptionChain? = null,
val metrics: NseMetrics? = null,
val loading: Boolean = false,
val error: String? = null
)

class McxViewModel(
private val repository: McxRepository = McxRepository()
) : ViewModel() {

```
private val _uiState = MutableStateFlow(McxUiState())

val uiState: StateFlow<McxUiState> =
    _uiState.asStateFlow()

init {
    loadExpiries()
}

fun setSymbol(symbol: String) {
    val normalized = symbol.trim().uppercase()

    if (normalized.isEmpty()) {
        return
    }

    _uiState.value = _uiState.value.copy(
        symbol = normalized,
        expiries = emptyList(),
        selectedExpiry = null,
        chain = null,
        metrics = null,
        error = null
    )

    loadExpiries()
}

fun loadExpiries() {
    val symbol = _uiState.value.symbol

    viewModelScope.launch {
        _uiState.value = _uiState.value.copy(
            loading = true,
            error = null
        )

        try {
            val expiries = repository.getExpiries(symbol)

            if (expiries.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    expiries = emptyList(),
                    selectedExpiry = null,
                    loading = false,
                    error = "No MCX expiries found for $symbol."
                )
                return@launch
            }

            val currentSelected =
                _uiState.value.selectedExpiry

            val selected =
                if (
                    currentSelected != null &&
                    expiries.contains(currentSelected)
                ) {
                    currentSelected
                } else {
                    expiries.first()
                }

            _uiState.value = _uiState.value.copy(
                expiries = expiries,
                selectedExpiry = selected,
                loading = false,
                error = null
            )

            loadOptionChain(selected)

        } catch (exception: Exception) {
            _uiState.value = _uiState.value.copy(
                loading = false,
                error = exception.message
                    ?: "Unable to load MCX expiry dates."
            )
        }
    }
}

fun selectExpiry(expiry: String) {
    val normalizedExpiry = expiry.trim()

    if (normalizedExpiry.isEmpty()) {
        return
    }

    _uiState.value = _uiState.value.copy(
        selectedExpiry = normalizedExpiry,
        error = null
    )

    loadOptionChain(normalizedExpiry)
}

fun loadOptionChain(
    expiry: String? = _uiState.value.selectedExpiry
) {
    val state = _uiState.value
    val symbol = state.symbol
    val selectedExpiry = expiry

    if (selectedExpiry.isNullOrBlank()) {
        _uiState.value = state.copy(
            loading = false,
            error = "Please select an MCX expiry."
        )
        return
    }

    viewModelScope.launch {
        _uiState.value = _uiState.value.copy(
            selectedExpiry = selectedExpiry,
            loading = true,
            error = null
        )

        try {
            val chain = repository.getOptionChain(
                symbol = symbol,
                expiry = selectedExpiry
            )

            val metrics = MetricsCalculator.calculate(
                chain = chain,
                vix = null
            )

            _uiState.value = _uiState.value.copy(
                chain = chain,
                metrics = metrics,
                loading = false,
                error = null
            )

        } catch (exception: Exception) {
            _uiState.value = _uiState.value.copy(
                loading = false,
                error = exception.message
                    ?: "Unable to load MCX option-chain data."
            )
        }
    }
}

fun refresh() {
    val expiry = _uiState.value.selectedExpiry

    if (expiry.isNullOrBlank()) {
        loadExpiries()
    } else {
        loadOptionChain(expiry)
    }
}

fun clearError() {
    _uiState.value = _uiState.value.copy(
        error = null
    )
}

fun retry() {
    val state = _uiState.value

    if (state.selectedExpiry.isNullOrBlank()) {
        loadExpiries()
    } else {
        loadOptionChain(state.selectedExpiry)
    }
}
```

}
