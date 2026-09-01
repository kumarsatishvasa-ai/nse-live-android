```kotlin
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
import java.util.Calendar
import java.util.TimeZone

data class NseUiState(
    val metrics: NseMetrics? = null,
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val error: String? = null
)

class NseViewModel : ViewModel() {

    private val repository = NseRepository()

    private val _uiState =
        MutableStateFlow(NseUiState())

    val uiState: StateFlow<NseUiState> =
        _uiState.asStateFlow()

    private var autoRefreshJob: Job? = null

    init {
        startAutoRefresh()
    }

    fun refresh() {
        if (_uiState.value.loading) return

        viewModelScope.launch {
            loadData(false)
        }
    }

    fun manualRefresh() {
        if (_uiState.value.loading) return

        viewModelScope.launch {
            loadData(true)
        }
    }

    private suspend fun loadData(
        manual: Boolean
    ) {
        _uiState.value =
            _uiState.value.copy(
                loading = true,
                refreshing = manual,
                error = null
            )

        try {

            val result =
                repository.loadNifty()

            _uiState.value =
                _uiState.value.copy(
                    metrics = result,
                    loading = false,
                    refreshing = false,
                    error = null
                )

        } catch (e: Exception) {

            _uiState.value =
                _uiState.value.copy(
                    loading = false,
                    refreshing = false,
                    error =
                        e.message
                            ?: "Unable to retrieve NSE data."
                )
        }
    }

    private fun startAutoRefresh() {

        autoRefreshJob?.cancel()

        autoRefreshJob =
            viewModelScope.launch {

                while (isActive) {

                    if (isMarketHours()) {

                        if (!_uiState.value.loading) {
                            loadData(false)
                        }

                        delay(30_000)

                    } else {

                        delay(60_000)
                    }
                }
            }
    }

    override fun onCleared() {
        autoRefreshJob?.cancel()
        super.onCleared()
    }

    companion object {

        fun isMarketHours(): Boolean {

            val calendar =
                Calendar.getInstance(
                    TimeZone.getTimeZone(
                        "Asia/Kolkata"
                    )
                )

            val day =
                calendar.get(Calendar.DAY_OF_WEEK)

            if (
                day == Calendar.SATURDAY ||
                day == Calendar.SUNDAY
            ) {
                return false
            }

            val hour =
                calendar.get(Calendar.HOUR_OF_DAY)

            val minute =
                calendar.get(Calendar.MINUTE)

            val totalMinutes =
                hour * 60 + minute

            return totalMinutes >= 555 &&
                    totalMinutes <= 930
        }
    }
}
```
