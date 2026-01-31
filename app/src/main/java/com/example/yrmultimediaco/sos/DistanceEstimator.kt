package com.example.yrmultimediaco.sos

import kotlin.math.roundToInt

data class DistanceEstimate(
    val minMeters: Int,
    val maxMeters: Int
)

enum class LinkQuality {
    EXCELLENT, GOOD, WEAK, CRITICAL, LOST
}

fun estimateDistanceRange(rssiSamples: List<Int>): DistanceEstimate {
    val txPower = -59
    val n = 2.7

    val minRssi = rssiSamples.minOrNull() ?: return DistanceEstimate(0, 0)
    val maxRssi = rssiSamples.maxOrNull() ?: return DistanceEstimate(0, 0)

    val minDist =
        Math.pow(10.0, (txPower - maxRssi) / (10 * n))
    val maxDist =
        Math.pow(10.0, (txPower - minRssi) / (10 * n))

    return DistanceEstimate(
        minMeters = minDist.roundToInt(),
        maxMeters = maxDist.roundToInt()
    )
}

fun rssiToQuality(rssi: Int): LinkQuality = when {
    rssi >= -60 -> LinkQuality.EXCELLENT
    rssi >= -70 -> LinkQuality.GOOD
    rssi >= -80 -> LinkQuality.WEAK
    rssi >= -90 -> LinkQuality.CRITICAL
    else -> LinkQuality.LOST
}
