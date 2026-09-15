package com.nselive.app

data class NseMetrics(
    val pcr: Double = 0.0,
    val maxPain: Double = 0.0,
    val gammaFlip: Double = 0.0,
    val callWall: Double = 0.0,
    val putWall: Double = 0.0,
    val expectedMove: Double = 0.0,
    val underlyingValue: Double = 0.0,
    val totalCallOi: Double = 0.0,
    val totalPutOi: Double = 0.0,
    val totalCallVolume: Double = 0.0,
    val totalPutVolume: Double = 0.0,
    val atmStrike: Double = 0.0,
    val timestamp: String = "",
    val expiry: String = ""
)
