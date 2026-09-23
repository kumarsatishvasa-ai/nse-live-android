package com.nselive.app.ema

class EmaCandleBuilder(
    private val timeframe: EmaTimeframe
) {

    private var currentCandle: EmaCandle? = null

    fun addPrice(
        timestamp: Long,
        price: Double
    ): EmaCandle? {

        val interval =
            timeframe.minutes * 60_000L

        val candleStart =
            (timestamp / interval) * interval

        val current =
            currentCandle

        // First price
        if (current == null) {

            currentCandle =
                EmaCandle(
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

            currentCandle =
                current.copy(
                    high =
                        maxOf(
                            current.high,
                            price
                        ),

                    low =
                        minOf(
                            current.low,
                            price
                        ),

                    close = price
                )

            return null
        }

        // Previous candle is now closed
        val closedCandle =
            current

        currentCandle =
            EmaCandle(
                startTime = candleStart,
                open = price,
                high = price,
                low = price,
                close = price
            )

        return closedCandle
    }
}
