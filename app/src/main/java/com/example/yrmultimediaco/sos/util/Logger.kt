package com.example.yrmultimediaco.sos.util

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

object Logger {

    private val formatter =
        SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun mesh(message: String) {
        log(Constants.LOG_TAG_MESH, message)
    }

    fun location(message: String) {
        log(Constants.LOG_TAG_LOCATION, message)
    }

    fun gateway(message: String) {
        log(Constants.LOG_TAG_GATEWAY, message)
    }

    fun discovery(message: String) {
        log(Constants.LOG_TAG_DISCOVERY, message)
    }

    fun connection(message: String) {
        log(Constants.LOG_TAG_CONNECTION, message)
    }

    private fun log(tag: String, message: String) {
        val timestamped =
            "[${formatter.format(Date())}] $message"

        Log.d(tag, timestamped)
        LogsBus.emit("$tag: $message")
    }
}
