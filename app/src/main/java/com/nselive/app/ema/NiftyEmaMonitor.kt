package com.nselive.app.ema

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class NiftyEmaMonitor(
    private val fastPeriod: Int = 9,
    private val slowPeriod: Int = 20,
    private val intervalMs: Long = 60_000L,
    private val onSignal: ((EmaSignal) -> Unit)? = null,
    private val onEmaUpdate: ((Double?, Double?) -> Unit)? = null
) {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var monitorJob: Job? = null

    private val prices = mutableListOf<Double>()

    private var previousFastEma: Double? = null
    private var previousSlowEma: Double? = null

    /**
     * Add the latest NIFTY price.
     *
     * The monitor calculates the fast and slow EMA whenever
     * enough price data is available.
     */
    fun addPrice(price: Double) {
        if (!price.isFinite() || price <= 0.0) return

        synchronized(prices) {
            prices.add(price)

            // Keep a reasonable amount of history.
            val maxHistory = maxOf(slowPeriod * 5, 200)

            if (prices.size > maxHistory) {
                prices.removeAt(0)
            }
        }

        calculateSignal()
    }

    /**
     * Add multiple historical prices.
     *
     * Prices must be in chronological order:
     * oldest -> newest.
     */
    fun addPrices(values: List<Double>) {
        values
            .filter { it.isFinite() && it > 0.0 }
            .forEach { price ->
                synchronized(prices) {
                    prices.add(price)
                }
            }

        val maxHistory = maxOf(slowPeriod * 5, 200)

        synchronized(prices) {
            while (prices.size > maxHistory) {
                prices.removeAt(0)
            }
        }

        calculateSignal()
    }

    /**
     * Calculate current fast/slow EMA and detect a crossover.
     */
    fun calculateSignal(): EmaSignal {
        val snapshot = synchronized(prices) {
            prices.toList()
        }

        if (snapshot.size < slowPeriod) {
            onEmaUpdate?.invoke(null, null)
            return EmaSignal.HOLD
        }

        val fastEma = EmaCalculator.calculate(
            prices = snapshot,
            period = fastPeriod
        )

        val slowEma = EmaCalculator.calculate(
            prices = snapshot,
            period = slowPeriod
        )

        onEmaUpdate?.invoke(fastEma, slowEma)

        val signal = EmaCrossDetector.detect(
            previousFast = previousFastEma,
            previousSlow = previousSlowEma,
            currentFast = fastEma,
            currentSlow = slowEma
        )

        previousFastEma = fastEma
        previousSlowEma = slowEma

        if (signal != EmaSignal.HOLD) {
            onSignal?.invoke(signal)
        }

        return signal
    }

    /**
     * Returns the latest EMA values.
     */
    fun getCurrentEma(): EmaResult? {
        val snapshot = synchronized(prices) {
            prices.toList()
        }

        if (snapshot.size < slowPeriod) {
            return null
        }

        val fastEma = EmaCalculator.calculate(
            prices = snapshot,
            period = fastPeriod
        ) ?: return null

        val slowEma = EmaCalculator.calculate(
            prices = snapshot,
            period = slowPeriod
        ) ?: return null

        val signal = when {
            fastEma > slowEma -> EmaSignal.BUY
            fastEma < slowEma -> EmaSignal.SELL
            else -> EmaSignal.HOLD
        }

        return EmaResult(
            emaFast = fastEma,
            emaSlow = slowEma,
            signal = signal
        )
    }

    /**
     * Start periodic monitoring.
     *
     * This does not fetch market data itself. Call addPrice()
     * from your market-data/API layer whenever a new price arrives.
     */
    fun start() {
        if (monitorJob?.isActive == true) return

        monitorJob = scope.launch {
            while (isActive) {
                calculateSignal()
                delay(intervalMs)
            }
        }
    }

    /**
     * Stop periodic monitoring.
     */
    fun stop() {
        monitorJob?.cancel()
        monitorJob = null
    }

    /**
     * Clear all EMA history and previous crossover state.
     */
    fun reset() {
        synchronized(prices) {
            prices.clear()
        }

        previousFastEma = null
        previousSlowEma = null
    }

    /**
     * Current number of stored price samples.
     */
    fun priceCount(): Int {
        return synchronized(prices) {
            prices.size
        }
    }

    /**
     * Whether the monitor has enough data to calculate both EMAs.
     */
    fun isReady(): Boolean {
        return synchronized(prices) {
            prices.size >= slowPeriod
        }
    }
}
