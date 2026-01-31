package com.example.yrmultimediaco.sos

object Util {
    fun ttlForPriority(priority: Int): Long {
        return when (priority) {
            0 -> 10 * 60 * 1000L
            1 -> 60 * 1000L
            else -> 5 * 60 * 1000L
        }
    }
}