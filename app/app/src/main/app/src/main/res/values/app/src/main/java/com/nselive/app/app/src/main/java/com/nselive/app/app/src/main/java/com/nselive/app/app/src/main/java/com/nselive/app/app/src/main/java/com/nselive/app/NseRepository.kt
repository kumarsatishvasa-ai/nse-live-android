```kotlin
package com.nselive.app

class NseRepository(
    private val api: NseApi = NseApi()
) {

    suspend fun loadNifty(): NseMetrics {

        val expiries =
            api.getExpiries("NIFTY")

        if (expiries.isEmpty()) {
            throw NseApiException(
                "No NIFTY expiry was returned by NSE."
            )
        }

        /*
         * NSE normally returns the nearest expiry first.
         */
        val expiry =
            expiries.first()

        val chain =
            api.getOptionChain(
                symbol = "NIFTY",
                expiry = expiry
            )

        val vix =
            try {
                api.getIndiaVix()
            } catch (_: Exception) {
                null
            }

        return MetricsCalculator.calculate(
            chain = chain,
            vix = vix
        )
    }
}
```
