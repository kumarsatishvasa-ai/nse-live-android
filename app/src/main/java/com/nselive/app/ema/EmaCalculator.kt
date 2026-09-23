package com.nselive.app.ema

object EmaCalculator {

    /**
     * Calculates the latest Exponential Moving Average (EMA).
     *
     * @param prices Price values in chronological order (oldest -> newest).
     * @param period EMA period, for example 9, 20, or 50.
     * @return Latest EMA value, or null when there is insufficient data.
     */
    fun calculate(
        prices: List<Double>,
        period: Int
    ): Double? {
        if (period <= 0) return null
        if (prices.size < period) return null

        val multiplier = 2.0 / (period + 1)

        var ema = prices
            .take(period)
            .average()

        for (i in period until prices.size) {
            ema = ((prices[i] - ema) * multiplier) + ema
        }

        return ema
    }

    /**
     * Calculates EMA values for the supplied price series.
     *
     * The first returned value is the SMA of the first [period] prices.
     */
    fun calculateSeries(
        prices: List<Double>,
        period: Int
    ): List<Double> {
        if (period <= 0) return emptyList()
        if (prices.size < period) return emptyList()

        val multiplier = 2.0 / (period + 1)
        val result = mutableListOf<Double>()

        var ema = prices
            .take(period)
            .average()

        result.add(ema)

        for (i in period until prices.size) {
            ema = ((prices[i] - ema) * multiplier) + ema
            result.add(ema)
        }

        return result
    }

    /**
     * Calculates the EMA from a collection of numeric values.
     */
    fun calculate(
        prices: DoubleArray,
        period: Int
    ): Double? {
        return calculate(prices.toList(), period)
    }

    /**
     * Calculates EMA series from a collection of numeric values.
     */
    fun calculateSeries(
        prices: DoubleArray,
        period: Int
    ): List<Double> {
        return calculateSeries(prices.toList(), period)
    }
}
