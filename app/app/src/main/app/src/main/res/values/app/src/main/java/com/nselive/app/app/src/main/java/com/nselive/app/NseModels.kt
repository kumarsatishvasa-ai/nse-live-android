```kotlin
package com.nselive.app

data class OptionContract(
    val strikePrice: Double,
    val expiryDate: String,

    val callOi: Double = 0.0,
    val callOiChange: Double = 0.0,
    val callVolume: Double = 0.0,
    val callIv: Double = 0.0,
    val callLtp: Double = 0.0,

    val putOi: Double = 0.0,
    val putOiChange: Double = 0.0,
    val putVolume: Double = 0.0,
    val putIv: Double = 0.0,
    val putLtp: Double = 0.0
)

data class OptionChain(
    val underlyingValue: Double,
    val timestamp: String,
    val contracts: List<OptionContract>
)

data class IndiaVix(
    val value: Double,
    val changePct: Double,
    val open: Double
)

data class NseResult(
    val optionChain: OptionChain? = null,
    val indiaVix: IndiaVix? = null,
    val error: String? = null
)
```
