package com.example.nselive.ema

class CandleAggregator(
    private val timeframe: TimeFrame
) {

    private var currentCandle: Candle? = null

    /**
     * Returns a CLOSED candle when the timeframe completes.
     * Otherwise returns null.
     */
    fun addPrice(
        timestamp: Long,
        price: Double
    ): Candle? {

        val intervalMillis =
            timeframe.minutes * 60_000L

        val candleStart =
            (timestamp / intervalMillis) * intervalMillis

        val current = currentCandle

        // First price
        if (current == null) {

            currentCandle = Candle(
                startTime = candleStart,
                open = price,
                high = price,
                low = price,
                close = price
            )

            return null
        }

        // Same candle
        if (current.startTime == candleStart) {

            currentCandle = current.copy(
                high = maxOf(current.high, price),
                low = minOf(current.low, price),
                close = price
            )

            return null
        }

        // New candle started.
        // Therefore the previous candle is CLOSED.
        val closedCandle = current

        currentCandle = Candle(
            startTime = candleStart,
            open = price,
            high = price,
            low = price,
            close = price
        )

        return closedCandle
    }
}
