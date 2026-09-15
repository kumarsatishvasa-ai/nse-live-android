package com.nselive.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class McxUiState(
    val symbol: String = "CRUDEOIL",
    val expiries: List<String> = emptyList(),
    val selectedExpiry: String = "",
    val metrics: NseMetrics = NseMetrics(),
    val loading: Boolean = false,
    val error: String? = null
)

class McxViewModel : ViewModel() {

    private val repository = McxRepository()

    private val _uiState =
        MutableStateFlow(
            McxUiState()
        )

    val uiState: StateFlow<McxUiState> =
        _uiState.asStateFlow()

    private var refreshJob: Job? = null

    init {
        loadExpiries("CRUDEOIL")
    }

    fun selectSymbol(symbol: String) {

        refreshJob?.cancel()

        _uiState.value =
            McxUiState(
                symbol = symbol
            )

        loadExpiries(symbol)
    }

    fun selectExpiry(expiry: String) {

        _uiState.value =
            _uiState.value.copy(
                selectedExpiry = expiry,
                error = null
            )

        startLiveRefresh()
    }

    private fun loadExpiries(symbol: String) {

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    loading = true,
                    error = null
                )

            try {

                val expiries =
                    repository.getExpiries(symbol)

                val firstExpiry =
                    expiries.firstOrNull() ?: ""

                _uiState.value =
                    _uiState.value.copy(
                        expiries = expiries,
                        selectedExpiry = firstExpiry,
                        loading = false,
                        error = null
                    )

                if (firstExpiry.isNotBlank()) {
                    startLiveRefresh()
                }

            } catch (e: Exception) {

                _uiState.value =
                    _uiState.value.copy(
                        loading = false,
                        error =
                            e.message
                                ?: "Unable to load MCX expiry dates"
                    )
            }
        }
    }

    private fun startLiveRefresh() {

        refreshJob?.cancel()

        refreshJob =
            viewModelScope.launch {

                while (isActive) {

                    if (
                        _uiState.value
                            .selectedExpiry
                            .isNotBlank()
                    ) {

                        loadChain()

                        delay(30_000)

                    } else {

                        delay(1_000)
                    }
                }
            }
    }

    private suspend fun loadChain() {

        val state =
            _uiState.value

        _uiState.value =
            state.copy(
                loading = true,
                error = null
            )

        try {

            val metrics =
                repository.getMetrics(
                    symbol = state.symbol,
                    expiry = state.selectedExpiry
                )

            _uiState.value =
                _uiState.value.copy(
                    metrics = metrics,
                    loading = false,
                    error = null
                )

        } catch (e: Exception) {

            _uiState.value =
                _uiState.value.copy(
                    loading = false,
                    error =
                        e.message
                            ?: "MCX data request failed"
                )
        }
    }

    fun refreshNow() {

        viewModelScope.launch {
            loadChain()
        }
    }

    override fun onCleared() {

        refreshJob?.cancel()

        super.onCleared()
    }
}
