package com.example.yrmultimediaco.sos

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import com.google.gson.Gson

class MeshManager(
    context: Context,
    private val log: (String) -> Unit
) {

    private val client = Nearby.getConnectionsClient(context)
    private val SERVICE_ID = "EMERGENCY_MESH_V1"

    private val peers = mutableSetOf<String>()
    private val seenPackets = mutableSetOf<String>()

    private val endpointName =
        "${Build.MANUFACTURER}-${Build.MODEL}".take(20)

    fun start() {
        startAdvertising()
        startDiscovery()
        log("Mesh started")
    }

    private fun startAdvertising() {
        log("Starting advertising as $endpointName")

        client.startAdvertising(
            endpointName,
            SERVICE_ID,
            lifecycleCallback,
            AdvertisingOptions.Builder()
                .setStrategy(Strategy.P2P_CLUSTER)
                .build()
        )
    }

    private fun startDiscovery() {
        log("Starting discovery...")

        client.startDiscovery(
            SERVICE_ID,
            discoveryCallback,
            DiscoveryOptions.Builder()
                .setStrategy(Strategy.P2P_CLUSTER)
                .build()
        )
    }

    private val discoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(id: String, info: DiscoveredEndpointInfo) {
            log("FOUND endpoint: ${info.endpointName} ($id)")
            client.requestConnection(endpointName, id, lifecycleCallback)
        }

        override fun onEndpointLost(id: String) {
            peers.remove(id)
        }
    }

    private val lifecycleCallback = object : ConnectionLifecycleCallback() {

        override fun onConnectionInitiated(id: String, info: ConnectionInfo) {
            log("CONNECTION INITIATED with $id")
            client.acceptConnection(id, payloadCallback)
        }

        override fun onConnectionResult(id: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                peers.add(id)
                log("CONNECTED: $id")
            } else {
                log("CONNECTION FAILED: ${result.status.statusCode}")
            }
        }

        override fun onDisconnected(id: String) {
            peers.remove(id)
            log("DISCONNECTED: $id")
        }
    }

    private val payloadCallback = object : PayloadCallback() {

        override fun onPayloadReceived(id: String, payload: Payload) {
            val bytes = payload.asBytes() ?: return
            val packet = Gson().fromJson(String(bytes), Packet::class.java)

            // ACK receive
            if (packet.isAck) {
                log("✅ ACK RECEIVED for packet ${packet.targetPacketId}")
                return
            }

            // Loop protection
            if (seenPackets.contains(packet.id)) return
            seenPackets.add(packet.id)

            log("[RECEIVED] ${packet.message} TTL=${packet.ttl}")

            //REVEAL CONDITION (2 devices case)
            if (packet.sourceDevice != endpointName) {
                log("🚨 SOS REVEALED on THIS DEVICE")
                sendAck(packet) // IMPORTANT
            }

            if (packet.ttl <= 0) {
                log("[DROP] TTL expired")
                return
            }

            packet.ttl--
            forward(packet)
        }

        override fun onPayloadTransferUpdate(
            id: String,
            update: PayloadTransferUpdate
        ) {}
    }

    // Activity calls THIS
    fun send(packet: Packet) {
        if (seenPackets.contains(packet.id)) return
        seenPackets.add(packet.id)
        forward(packet)
    }

    private fun forward(packet: Packet) {
        val bytes = Gson().toJson(packet).toByteArray()
        val payload = Payload.fromBytes(bytes)

        peers.forEach {
            client.sendPayload(it, payload)
        }

        log("[FORWARD] TTL=${packet.ttl}")
    }

    private fun sendAck(original: Packet) {
        val ack = Packet(
            message = "ACK",
            priority = 3,
            ttl = 2,

            lat = null,
            lng = null,

            sourceDevice = endpointName,
            sourceTimeMillis = System.currentTimeMillis(),

            isAck = true,
            targetPacketId = original.id
        )

        forward(ack)
        log("📩 ACK sent for ${original.id}")
    }

}

