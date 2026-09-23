package com.nselive.app.ema

object EmaCalculator {

    fun calculate(
        prices: List<Double>,
        period: Int
    ): Double? {

        if (prices.size < period) {
            return null
        }

        val multiplier =
            2.0 / (period + 1)

        // First EMA = SMA
        var ema =
            prices
                .take(period)
                .average()

        for (i in period until prices.size) {

            ema =
                ((prices[i] - ema) * multiplier) +
                        ema
        }

        return ema
    }
}
