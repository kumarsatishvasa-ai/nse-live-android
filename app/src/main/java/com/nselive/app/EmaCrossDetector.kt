package com.example.nselive

enum class EmaSignal {
    BULLISH,
    BEARISH,
    NONE
}

class EmaCrossDetector {

    private var previousEma9: Double? = null
    private var previousEma21: Double? = null

    fun checkCross(
        ema9: Double,
        ema21: Double
    ): EmaSignal {

        val old9 = previousEma9
        val old21 = previousEma21

        previousEma9 = ema9
        previousEma21 = ema21

        if (old9 == null || old21 == null) {
            return EmaSignal.NONE
        }

        // 9 EMA crossed ABOVE 21 EMA
        if (old9 <= old21 && ema9 > ema21) {
            return EmaSignal.BULLISH
        }

        // 9 EMA crossed BELOW 21 EMA
        if (old9 >= old21 && ema9 < ema21) {
            return EmaSignal.BEARISH
        }

        return EmaSignal.NONE
    }

    fun reset() {
        previousEma9 = null
        previousEma21 = null
    }
}
