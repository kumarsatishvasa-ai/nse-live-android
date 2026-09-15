package com.nselive.app

class McxRepository(
    private val api: McxApi = McxApi()
) {

    suspend fun getExpiries(
        symbol: String
    ): List<String> {

        return api.getExpiries(symbol)
    }

    suspend fun getOptionChain(
        symbol: String,
        expiry: String
    ): OptionChain {

        return api.getOptionChain(
            symbol = symbol,
            expiry = expiry
        )
    }

    suspend fun getMetrics(
        symbol: String,
        expiry: String
    ): NseMetrics {

        val chain =
            api.getOptionChain(
                symbol = symbol,
                expiry = expiry
            )

        return MetricsCalculator.calculate(
            chain = chain,
            vix = null
        )
    }
}
