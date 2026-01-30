package com.example.yrmultimediaco.sos.data

import android.os.Build
import com.example.yrmultimediaco.sos.PacketType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class Packet(
    val id: String = UUID.randomUUID().toString(),

    val type: PacketType = PacketType.SOS,

    val message: String,

    val priority: Int,

    val lat: Double? = null,         // nullable (GPS off case)
    val lng: Double? = null,

    val payloadUserId: String,

    val sourceDevice: String = "${Build.MANUFACTURER}-${Build.MODEL}",
    val sourceTimeMillis: Long,

    val createdAt: String = SimpleDateFormat(
        "yyyy-MM-dd HH:mm:ss",
        Locale.US
    ).format(Date()),

    val expiredAt: Long,

    val isAck: Boolean = false,
    val targetPacketId: String? = null,

    val originalSenderId: String? = null
)