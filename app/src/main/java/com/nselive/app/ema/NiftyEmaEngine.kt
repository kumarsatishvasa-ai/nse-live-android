package com.nselive.app.ema

class NiftyEmaEngine(
    private val onEmaUpdate: (
        timeframe: EmaTimeframe,
        ema9: Double?,
        ema21: Double?,
        signal: EmaSignal
    ) -> Unit
) {

    private val oneMinuteBuilder =
        EmaCandleBuilder(
            EmaTimeframe.ONE_MINUTE
        )

    private val threeMinuteBuilder =
        EmaCandleBuilder(
            EmaTimeframe.THREE_MINUTE
        )

    private val oneMinuteCandles =
        mutableListOf<EmaCandle>()

    private val threeMinuteCandles =
        mutableListOf<EmaCandle>()

    private var previous1mEma9: Double? = null
    private var previous1mEma21: Double? = null

    private var previous3mEma9: Double? = null
    private var previous3mEma21: Double? = null

    /**
     * Call this every time your app receives
     * a new NIFTY underlying price.
     */
    fun onPrice(
        timestamp: Long,
        price: Double
    ) {

        processOneMinute(
            timestamp,
            price
        )

        processThreeMinute(
            timestamp,
            price
        )
    }

    private fun processOneMinute(
        timestamp: Long,
        price: Double
    ) {

        val closed =
            oneMinuteBuilder.addPrice(
                timestamp,
                price
            )

        if (closed == null) {
            return
        }

        oneMinuteCandles.add(
            closed
        )

        calculate(
            timeframe =
                EmaTimeframe.ONE_MINUTE,

            candles =
                oneMinuteCandles
        )
    }

    private fun processThreeMinute(
        timestamp: Long,
        price: Double
    ) {

        val closed =
            threeMinuteBuilder.addPrice(
                timestamp,
                price
            )

        if (closed == null) {
            return
        }

        threeMinuteCandles.add(
            closed
        )

        calculate(
            timeframe =
                EmaTimeframe.THREE_MINUTE,

            candles =
                threeMinuteCandles
        )
    }

    private fun calculate(
        timeframe: EmaTimeframe,
        candles: List<EmaCandle>
    ) {

        val closes =
            candles.map {
                it.close
            }

        val ema9 =
            EmaCalculator.calculate(
                closes,
                9
            )

        val ema21 =
            EmaCalculator.calculate(
                closes,
                21
            )

        if (
            ema9 == null ||
            ema21 == null
        ) {

            onEmaUpdate(
                timeframe,
                ema9,
                ema21,
                EmaSignal.NONE
            )

            return
        }

        val signal =
            detectCross(
                timeframe,
                ema9,
                ema21
            )

        onEmaUpdate(
            timeframe,
            ema9,
            ema21,
            signal
        )
    }

    private fun detectCross(
        timeframe: EmaTimeframe,
        ema9: Double,
        ema21: Double
    ): EmaSignal {

        return when (timeframe) {

            EmaTimeframe.ONE_MINUTE -> {

                val old9 =
                    previous1mEma9

                val old21 =
                    previous1mEma21

                previous1mEma9 =
                    ema9

                previous1mEma21 =
                    ema21

                if (
                    old9 != null &&
                    old21 != null
                ) {

                    if (
                        old9 <= old21 &&
                        ema9 > ema21
                    ) {
                        EmaSignal.BULLISH

                    } else if (
                        old9 >= old21 &&
                        ema9 < ema21
                    ) {
                        EmaSignal.BEARISH

                    } else {
                        EmaSignal.NONE
                    }

                } else {
                    EmaSignal.NONE
                }
            }

            EmaTimeframe.THREE_MINUTE -> {

                val old9 =
                    previous3mEma9

                val old21 =
                    previous3mEma21

                previous3mEma9 =
                    ema9

                previous3mEma21 =
                    ema21

                if (
                    old9 != null &&
                    old21 != null
                ) {

                    if (
                        old9 <= old21 &&
                        ema9 > ema21
                    ) {
                        EmaSignal.BULLISH

                    } else if (
                        old9 >= old21 &&
                        ema9 < ema21
                    ) {
                        EmaSignal.BEARISH

                    } else {
                        EmaSignal.NONE
                    }

                } else {
                    EmaSignal.NONE
                }
            }
        }
    }

    fun reset() {

        oneMinuteCandles.clear()
        threeMinuteCandles.clear()

        previous1mEma9 = null
        previous1mEma21 = null

        previous3mEma9 = null
        previous3mEma21 = null
    }
}
