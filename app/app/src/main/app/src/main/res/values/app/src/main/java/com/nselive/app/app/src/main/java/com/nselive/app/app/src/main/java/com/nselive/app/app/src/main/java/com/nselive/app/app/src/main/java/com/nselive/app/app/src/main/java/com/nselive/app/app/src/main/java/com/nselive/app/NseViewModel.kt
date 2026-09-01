```kotlin
package com.nselive.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

class NseViewModel : ViewModel() {

    private val repository = NseRepository()

    var metrics: NseMetrics? =
        null
        private set

    var loading: Boolean =
        false
        private set

    var refreshing: Boolean =
        false
        private set

    var error: String? =
        null
        private set

    private var refreshJob: Job? = null

    init {
        startAutoRefresh()
    }

    fun refresh() {

        if (loading) {
            return
        }

        viewModelScope.launch {
            loadData()
        }
    }

    private suspend fun loadData() {

        loading = true
        error = null

        try {

            metrics =
                repository.loadNifty()

        } catch (e: Exception) {

            error =
                e.message
                    ?: "Unable to retrieve NSE data."

        } finally {

            loading = false
            refreshing = false
        }
    }

    fun manualRefresh() {

        if (loading) {
            return
        }

        viewModelScope.launch {

            refreshing = true

            loadData()
        }
    }

    private fun startAutoRefresh() {

        refreshJob?.cancel()

        refreshJob =
            viewModelScope.launch {

                while (isActive) {

                    if (isMarketHours()) {

                        if (!loading) {
                            loadData()
                        }

                        delay(30_000)

                    } else {

                        // Check market state once per minute
                        // while the exchange is closed.
                        delay(60_000)
                    }
                }
            }
    }

    override fun onCleared() {

        refreshJob?.cancel()

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
                calendar.get(
                    Calendar.DAY_OF_WEEK
                )

            if (
                day == Calendar.SATURDAY ||
                day == Calendar.SUNDAY
            ) {
                return false
            }

            val hour =
                calendar.get(
                    Calendar.HOUR_OF_DAY
                )

            val minute =
                calendar.get(
                    Calendar.MINUTE
                )

            val totalMinutes =
                hour * 60 + minute

            return totalMinutes >=
                    (9 * 60 + 15) &&
                    totalMinutes <=
                    (15 * 60 + 30)
        }
    }
}
```
