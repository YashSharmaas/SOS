package com.example.yrmultimediaco.sos

fun ttlForPriority(priority: Int): Long {
    return when (priority) {
        0 -> 10 * 60 * 1000L
        1 -> 2 * 60 * 1000L
        else -> 7 * 60 * 1000L
    }
}