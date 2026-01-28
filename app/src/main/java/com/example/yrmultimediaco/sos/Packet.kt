package com.example.yrmultimediaco.sos

import android.os.Build
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class Packet(
    val id: String = UUID.randomUUID().toString(),

    val type: PacketType = PacketType.SOS,

    val message: String,

    val priority: Int,
    var ttl: Int,             // hop count

    val lat: Double?,         // nullable (GPS off case)
    val lng: Double?,

    val sourceDevice: String = "${Build.MANUFACTURER}-${Build.MODEL}",
    val sourceTimeMillis: Long,

    val createdAt: String = SimpleDateFormat(
        "yyyy-MM-dd HH:mm:ss",
        Locale.US
    ).format(Date()),

    val isAck: Boolean = false,
    val targetPacketId: String? = null
)


