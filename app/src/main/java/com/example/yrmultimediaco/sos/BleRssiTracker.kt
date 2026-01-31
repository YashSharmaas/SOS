package com.example.yrmultimediaco.sos

class BleRssiTracker {

    private val samples =
        mutableMapOf<String, MutableList<Int>>()

    fun addSample(endpointId: String, rssi: Int) {
        val list = samples.getOrPut(endpointId) {
            mutableListOf()
        }

        list.add(rssi)
        if (list.size > 10) list.removeAt(0)
    }

    fun getDistance(endpointId: String): DistanceEstimate? {
        val data = samples[endpointId] ?: return null
        if (data.size < 3) return null
        return estimateDistanceRange(data)
    }

    fun getQuality(endpointId: String): LinkQuality? {
        val avg = samples[endpointId]?.average() ?: return null

        return when {
            avg > -60 -> LinkQuality.EXCELLENT
            avg > -70 -> LinkQuality.GOOD
            avg > -80 -> LinkQuality.WEAK
            avg > -90 -> LinkQuality.CRITICAL
            else -> LinkQuality.LOST
        }
    }

    fun getMostLikelyDevice(): String? {
        val now = System.currentTimeMillis()
        // Find the device with the most samples recently
        return samples.entries
            .filter { it.value.isNotEmpty() }
            .maxByOrNull { it.value.last() } // Sort by strongest RSSI (last sample)
            ?.key
    }
}
