package com.example.nselive

class EmaCalculator {

    /**
     * Calculates EMA for the supplied prices.
     */
    fun calculateEma(prices: List<Double>, period: Int): Double? {
        if (prices.size < period) return null

        val multiplier = 2.0 / (period + 1)

        // Initial EMA = SMA of first `period` values
        var ema = prices.take(period).average()

        for (i in period until prices.size) {
            ema = ((prices[i] - ema) * multiplier) + ema
        }

        return ema
    }
}
