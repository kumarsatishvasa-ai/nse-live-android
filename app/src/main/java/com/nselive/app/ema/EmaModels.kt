package com.nselive.app.ema

enum class EmaTimeframe(
    val minutes: Int
) {
    ONE_MINUTE(1),
    THREE_MINUTE(3)
}

enum class EmaSignal {
    BULLISH,
    BEARISH,
    NONE
}

data class EmaCandle(
    val startTime: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double
)

data class EmaInfo(
    val ema9: Double?,
    val ema21: Double?,
    val signal: EmaSignal = EmaSignal.NONE
)
