package com.example.nselive.ema

class NiftyEmaMonitor(
    private val onSignal: (
        timeframe: TimeFrame,
        signal: EmaSignal,
        ema9: Double,
        ema21: Double
    ) -> Unit
) {

    private val detector = EmaCrossDetector()

    private val oneMinuteAggregator =
        CandleAggregator(TimeFrame.ONE_MINUTE)

    private val threeMinuteAggregator =
        CandleAggregator(TimeFrame.THREE_MINUTE)

    private val oneMinuteCandles =
        mutableListOf<Candle>()

    private val threeMinuteCandles =
        mutableListOf<Candle>()

    fun onNewPrice(
        timestamp: Long,
        price: Double
    ) {

        // -----------------------------
        // 1 MINUTE
        // -----------------------------

        val closed1m =
            oneMinuteAggregator.addPrice(
                timestamp,
                price
            )

        if (closed1m != null) {

            oneMinuteCandles.add(closed1m)

            checkEma(
                timeframe = TimeFrame.ONE_MINUTE,
                candles = oneMinuteCandles
            )
        }

        // -----------------------------
        // 3 MINUTE
        // -----------------------------

        val closed3m =
            threeMinuteAggregator.addPrice(
                timestamp,
                price
            )

        if (closed3m != null) {

            threeMinuteCandles.add(closed3m)

            checkEma(
                timeframe = TimeFrame.THREE_MINUTE,
                candles = threeMinuteCandles
            )
        }
    }

    private fun checkEma(
        timeframe: TimeFrame,
        candles: List<Candle>
    ) {

        val closes =
            candles.map { it.close }

        val ema9 =
            EmaCalculator.calculate(
                closes,
                9
            ) ?: return

        val ema21 =
            EmaCalculator.calculate(
                closes,
                21
            ) ?: return

        val signal =
            detector.check(
                timeframe,
                ema9,
                ema21
            )

        if (signal != EmaSignal.NONE) {

            onSignal(
                timeframe,
                signal,
                ema9,
                ema21
            )
        }
    }

    fun clear() {

        oneMinuteCandles.clear()
        threeMinuteCandles.clear()

        detector.reset(
            TimeFrame.ONE_MINUTE
        )

        detector.reset(
            TimeFrame.THREE_MINUTE
        )
    }
}
