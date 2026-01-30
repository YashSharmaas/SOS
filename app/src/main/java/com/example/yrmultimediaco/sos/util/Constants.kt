package com.example.yrmultimediaco.sos.util

/**
 * Application-wide constants.
 */
object Constants {

    // ---- Mesh / Nearby ----
    const val SERVICE_ID = "CRISIS_BRIDGE_MESH_V1"

    // ---- Packet / Networking ----
    const val MAX_PACKET_AGE_MS = 120_000L   // 2 minutes
    const val DEFAULT_SOS_TTL = 5
    const val DEFAULT_STATUS_TTL = 3

    // ---- Scheduler ----
    const val SCHEDULER_BUFFER_SIZE = 30

    // ---- Logging ----
    const val LOG_TAG_MESH = "MESH"
    const val LOG_TAG_LOCATION = "LOCATION"
    const val LOG_TAG_GATEWAY = "GATEWAY"
    const val LOG_TAG_DISCOVERY = "DISCOVERY"
    const val LOG_TAG_CONNECTION = "CONNECTION"

    // ---- Notification ----
    const val CHANNEL_ID = "CRISIS_MESH_CHANNEL"
    const val NOTIFICATION_ID = 101
}
