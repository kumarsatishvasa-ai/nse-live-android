```kotlin
package com.nselive.app

import kotlin.math.abs
import kotlin.math.max

object MetricsCalculator {

    fun calculate(
        chain: OptionChain,
        vix: IndiaVix?
    ): NseMetrics {

        val contracts = chain.contracts

        val totalCallOi =
            contracts.sumOf { it.callOi }

        val totalPutOi =
            contracts.sumOf { it.putOi }

        val totalCallVolume =
            contracts.sumOf { it.callVolume }

        val totalPutVolume =
            contracts.sumOf { it.putVolume }

        val pcrOi =
            if (totalCallOi > 0.0)
                totalPutOi / totalCallOi
            else null

        val pcrVolume =
            if (totalCallVolume > 0.0)
                totalPutVolume / totalCallVolume
            else null

        val maxPain =
            calculateMaxPain(contracts)

        val callWall =
            contracts
                .maxByOrNull { it.callOi }
                ?.strikePrice

        val putWall =
            contracts
                .maxByOrNull { it.putOi }
                ?.strikePrice

        val gammaFlip =
            calculateGammaFlip(
                contracts,
                chain.underlyingValue
            )

        val expectedMove =
            calculateExpectedMove(
                chain.underlyingValue,
                vix
            )

        return NseMetrics(
            pcrOi = pcrOi,
            pcrVolume = pcrVolume,
            maxPain = maxPain,
            gammaFlip = gammaFlip,
            callWall = callWall,
            putWall = putWall,
            expectedMove = expectedMove,
            indiaVix = vix?.value,
            updatedAt = chain.timestamp
        )
    }

    /**
     * Max Pain:
     *
     * For every candidate strike, calculate the total option
     * payout that would occur if NIFTY expired at that strike.
     *
     * The strike with the smallest aggregate payout is Max Pain.
     */
    private fun calculateMaxPain(
        contracts: List<OptionContract>
    ): Double? {

        val strikes =
            contracts
                .map { it.strikePrice }
                .distinct()
                .sorted()

        if (strikes.isEmpty()) {
            return null
        }

        var bestStrike = strikes.first()
        var smallestPain = Double.POSITIVE_INFINITY

        for (expiryPrice in strikes) {

            var pain = 0.0

            for (contract in contracts) {

                val callIntrinsic =
                    max(
                        expiryPrice -
                                contract.strikePrice,
                        0.0
                    )

                val putIntrinsic =
                    max(
                        contract.strikePrice -
                                expiryPrice,
                        0.0
                    )

                pain +=
                    callIntrinsic *
                            contract.callOi

                pain +=
                    putIntrinsic *
                            contract.putOi
            }

            if (pain < smallestPain) {
                smallestPain = pain
                bestStrike = expiryPrice
            }
        }

        return bestStrike
    }

    /**
     * Approximate gamma flip using open-interest-weighted
     * gamma exposure.
     *
     * This is an analytical approximation because NSE's
     * option-chain endpoint does not directly provide dealer
     * positioning or a signed gamma field.
     */
    private fun calculateGammaFlip(
        contracts: List<OptionContract>,
        spot: Double
    ): Double? {

        if (spot <= 0.0 || contracts.isEmpty()) {
            return null
        }

        val candidates =
            contracts
                .map { it.strikePrice }
                .distinct()
                .sorted()

        var previousExposure: Double? = null
        var previousStrike: Double? = null

        for (strike in candidates) {

            var exposure = 0.0

            for (contract in contracts) {

                val distance =
                    abs(
                        contract.strikePrice -
                                strike
                    )

                /*
                 * Simple gamma proxy:
                 *
                 * ATM options have greater influence.
                 * Open interest determines magnitude.
                 *
                 * Calls and puts receive opposite signs
                 * to estimate the zero-crossing.
                 */
                val weighting =
                    1.0 /
                            (1.0 + distance / max(
                                spot * 0.01,
                                1.0
                            ))

                exposure +=
                    contract.callOi *
                            weighting

                exposure -=
                    contract.putOi *
                            weighting
            }

            val previous =
                previousExposure

            if (
                previous != null &&
                previousStrike != null &&
                previous * exposure < 0.0
            ) {

                return (
                    previousStrike +
                            strike
                ) / 2.0
            }

            previousExposure = exposure
            previousStrike = strike
        }

        /*
         * If no zero crossing is found, return the strike
         * closest to the smallest absolute exposure.
         */
        return candidates.minByOrNull { strike ->

            var exposure = 0.0

            for (contract in contracts) {

                val distance =
                    abs(
                        contract.strikePrice -
                                strike
                    )

                val weighting =
                    1.0 /
                            (1.0 + distance / max(
                                spot * 0.01,
                                1.0
                            ))

                exposure +=
                    contract.callOi *
                            weighting

                exposure -=
                    contract.putOi *
                            weighting
            }

            abs(exposure)
        }
    }

    /**
     * Expected move approximation using India VIX.
     *
     * VIX is annualised volatility.
     *
     * Expected move ≈ Spot × VIX% × sqrt(days / 365)
     *
     * For the displayed live value we use one trading day.
     */
    private fun calculateExpectedMove(
        spot: Double,
        vix: IndiaVix?
    ): Double? {

        if (
            spot <= 0.0 ||
            vix == null ||
            vix.value <= 0.0
        ) {
            return null
        }

        val annualVolatility =
            vix.value / 100.0

        val oneTradingDay =
            1.0 / 252.0

        return spot *
                annualVolatility *
                kotlin.math.sqrt(
                    oneTradingDay
                )
    }
}
```
