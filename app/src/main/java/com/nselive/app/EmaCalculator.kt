package com.nselive.app

import kotlin.math.abs

/**

EMA signal.
*/
enum class EmaSignal {
BULLISH,
BEARISH,
NEUTRAL
}

/**

Result of EMA calculation.
*/
data class EmaResult(
val emaFast: Double,
val emaSlow: Double,
val signal: EmaSignal
)

/**

EMA calculator.

Supports arbitrary candle timeframes.

For example:

1-minute candles:

EMA 9 / EMA 21

3-minute candles:

EMA 9 / EMA 21

The calculator itself does not care about timeframe.
*/
object EmaCalculator {

fun calculateEma(
prices: List<Double>,
period: Int
): Double? {

 if (period <= 0) {
     return null
 }

 if (prices.isEmpty()) {
     return null
 }

 if (prices.size < period) {
     return null
 }

 var ema =
     prices
         .take(period)
         .average()

 val multiplier =
     2.0 / (period + 1.0)

 for (index in period until prices.size) {

     val price =
         prices[index]

     ema =
         (price - ema) *
                 multiplier +
                 ema
 }

 return ema


}

fun calculate(
prices: List<Double>,
fastPeriod: Int = 9,
slowPeriod: Int = 21
): EmaResult? {

 if (prices.size < slowPeriod) {
     return null
 }

 val fast =
     calculateEma(
         prices = prices,
         period = fastPeriod
     )
         ?: return null

 val slow =
     calculateEma(
         prices = prices,
         period = slowPeriod
     )
         ?: return null

 val signal =
     when {
         fast > slow ->
             EmaSignal.BULLISH

         fast < slow ->
             EmaSignal.BEARISH

         else ->
             EmaSignal.NEUTRAL
     }

 return EmaResult(
     emaFast = fast,
     emaSlow = slow,
     signal = signal
 )


}
}

/**

Detects EMA crossovers.
*/
object EmaCrossDetector {

fun detect(
previousFast: Double?,
previousSlow: Double?,
currentFast: Double,
currentSlow: Double
): EmaSignal {

 if (
     previousFast == null ||
     previousSlow == null
 ) {
     return when {
         currentFast > currentSlow ->
             EmaSignal.BULLISH

         currentFast < currentSlow ->
             EmaSignal.BEARISH

         else ->
             EmaSignal.NEUTRAL
     }
 }

 /*
  * Bullish crossover:
  *
  * Previous:
  * fast <= slow
  *
  * Current:
  * fast > slow
  */
 if (
     previousFast <= previousSlow &&
     currentFast > currentSlow
 ) {
     return EmaSignal.BULLISH
 }

 /*
  * Bearish crossover:
  *
  * Previous:
  * fast >= slow
  *
  * Current:
  * fast < slow
  */
 if (
     previousFast >= previousSlow &&
     currentFast < currentSlow
 ) {
     return EmaSignal.BEARISH
 }

 return EmaSignal.NEUTRAL


}

fun currentSignal(
fast: Double,
slow: Double
): EmaSignal {

 return when {
     fast > slow ->
         EmaSignal.BULLISH

     fast < slow ->
         EmaSignal.BEARISH

     else ->
         EmaSignal.NEUTRAL
 }


}
}
