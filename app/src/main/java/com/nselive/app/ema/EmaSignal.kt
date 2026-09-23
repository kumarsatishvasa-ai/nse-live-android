package com.nselive.app

/**

EMA signal states.

BULLISH = fast EMA is above slow EMA

BEARISH = fast EMA is below slow EMA

NEUTRAL = fast EMA and slow EMA are equal
*/
enum class EmaSignal {
BULLISH,
BEARISH,
NEUTRAL
}
