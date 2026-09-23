package com.example.nselive.ema

enum class EmaSignal {
    BULLISH,
    BEARISH,
    NONE
}

data class EmaState(
    var previousEma9: Double? = null,
    var previousEma21: Double? = null,
    var lastSignal: EmaSignal = EmaSignal.NONE
)

class EmaCrossDetector {

    private val states = mutableMapOf(
        TimeFrame.ONE_MINUTE to EmaState(),
        TimeFrame.THREE_MINUTE to EmaState()
    )

    fun check(
        timeframe: TimeFrame,
        ema9: Double,
        ema21: Double
    ): EmaSignal {

        val state = states[timeframe]
            ?: EmaState().also {
                states[timeframe] = it
            }

        val old9 = state.previousEma9
        val old21 = state.previousEma21

        state.previousEma9 = ema9
        state.previousEma21 = ema21

        if (old9 == null || old21 == null) {
            return EmaSignal.NONE
        }

        // 9 EMA crossed ABOVE 21 EMA
        if (old9 <= old21 && ema9 > ema21) {

            state.lastSignal = EmaSignal.BULLISH

            return EmaSignal.BULLISH
        }

        // 9 EMA crossed BELOW 21 EMA
        if (old9 >= old21 && ema9 < ema21) {

            state.lastSignal = EmaSignal.BEARISH

            return EmaSignal.BEARISH
        }

        return EmaSignal.NONE
    }

    fun reset(timeframe: TimeFrame) {
        states[timeframe] = EmaState()
    }
}
