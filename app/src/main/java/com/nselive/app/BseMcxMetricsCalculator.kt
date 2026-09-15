package com.nselive.app

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

object BseMcxMetricsCalculator {

    fun calculate(
        chain: BseMcxOptionChain
    ): BseMcxMetrics {

        val contracts = chain.contracts

        if (contracts.isEmpty()) {
            return BseMcxMetrics(
                error = "No option contracts received"
            )
        }

        val callOi =
            contracts.sumOf { it.callOi }

        val putOi =
            contracts.sumOf { it.putOi }

        val callVolume =
            contracts.sumOf { it.callVolume }

        val putVolume =
            contracts.sumOf { it.putVolume }

        val pcrOi =
            if (callOi > 0.0) {
                putOi / callOi
            } else {
                null
            }

        val pcrVolume =
            if (callVolume > 0.0) {
                putVolume / callVolume
            } else {
                null
            }

        val maxPain =
            calculateMaxPain(contracts)

        val callWall =
            contracts
                .maxByOrNull { it.callOi }
                ?.strike

        val putWall =
            contracts
                .maxByOrNull { it.putOi }
                ?.strike

        val gammaFlip =
            calculateGammaFlip(
                contracts,
                chain.underlyingValue
            )

        /*
         * We deliberately do not use India VIX for MCX.
         *
         * Expected move is calculated from the
         * option-chain IV around the ATM strike.
         */
        val expectedMove =
            calculateExpectedMove(
                contracts,
                chain.underlyingValue
            )

        return BseMcxMetrics(
            pcrOi = pcrOi,
            pcrVolume = pcrVolume,
            maxPain = maxPain,
            gammaFlip = gammaFlip,
            callWall = callWall,
            putWall = putWall,
            expectedMove = expectedMove,
            updatedAt = chain.timestamp
        )
    }

    private fun calculateMaxPain(
        contracts: List<BseMcxOption>
    ): Double? {

        val strikes =
            contracts
                .map { it.strike }
                .distinct()
                .sorted()

        if (strikes.isEmpty()) {
            return null
        }

        var bestStrike = strikes.first()

        var smallestPain =
            Double.POSITIVE_INFINITY

        for (expiryPrice in strikes) {

            var pain = 0.0

            for (contract in contracts) {

                val callIntrinsic =
                    max(
                        expiryPrice -
                                contract.strike,
                        0.0
                    )

                val putIntrinsic =
                    max(
                        contract.strike -
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

                bestStrike =
                    expiryPrice
            }
        }

        return bestStrike
    }

    private fun calculateGammaFlip(
        contracts: List<BseMcxOption>,
        spot: Double
    ): Double? {

        if (
            spot <= 0.0 ||
            contracts.isEmpty()
        ) {
            return null
        }

        val strikes =
            contracts
                .map { it.strike }
                .distinct()
                .sorted()

        var previousExposure: Double? = null
        var previousStrike: Double? = null

        for (strike in strikes) {

            var exposure = 0.0

            for (contract in contracts) {

                val distance =
                    abs(
                        contract.strike -
                                strike
                    )

                val weighting =
                    1.0 /
                            (
                                1.0 +
                                        distance /
                                        max(
                                            spot * 0.01,
                                            1.0
                                        )
                            )

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

        return strikes.minByOrNull { strike ->

            var exposure = 0.0

            for (contract in contracts) {

                val distance =
                    abs(
                        contract.strike -
                                strike
                    )

                val weighting =
                    1.0 /
                            (
                                1.0 +
                                        distance /
                                        max(
                                            spot * 0.01,
                                            1.0
                                        )
                            )

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

    private fun calculateExpectedMove(
        contracts: List<BseMcxOption>,
        spot: Double
    ): Double? {

        if (
            spot <= 0.0 ||
            contracts.isEmpty()
        ) {
            return null
        }

        val atm =
            contracts.minByOrNull {
                abs(
                    it.strike - spot
                )
            } ?: return null

        val ivValues =
            listOf(
                atm.callIv,
                atm.putIv
            )
                .filter {
                    it > 0.0
                }

        if (ivValues.isEmpty()) {
            return null
        }

        val iv =
            ivValues.average()

        /*
         * One-day expected move.
         *
         * IV is annualised percentage volatility.
         */
        return spot *
                (iv / 100.0) *
                sqrt(1.0 / 252.0)
    }
}
