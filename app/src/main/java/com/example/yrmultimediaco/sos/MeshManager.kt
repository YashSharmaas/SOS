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
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities


class MeshManager(
    private val context: Context,
    private val log: (String) -> Unit
) {

    private val client = Nearby.getConnectionsClient(context)
    private val SERVICE_ID = "EMERGENCY_MESH_V1"
    private val peers = mutableSetOf<String>()
    private val seenPackets = mutableMapOf<String, Long>() // id -> expiredAt
    private val pendingPackets = mutableMapOf<String, Packet>() // id -> packet
    private val ackedPackets = mutableSetOf<String>() // SOS ids already ACKed

    private val endpointName =
        "${Build.MANUFACTURER}-${Build.MODEL}".take(20)

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            log("🌐 Internet available")
            handler.post { evaluateSystemState() }
        }
    }

    fun start() {
        startAdvertising()
        startDiscovery()
        handler.post(rebroadcastTask)
        handler.post(cleanupTask)
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
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

            val now = System.currentTimeMillis()

            if (now > packet.expiredAt) return
            if (seenPackets.containsKey(packet.id)) return

            seenPackets[packet.id] = packet.expiredAt

            if (packet.isAck) {
                pendingPackets.remove(packet.targetPacketId)

                if (packet.originalSenderId == endpointName) {
                    log("🎯 ORIGIN RECEIVED ACK — SOS SUCCESS")
                    pendingPackets.remove(packet.id)
                } else {
                    pendingPackets[packet.id] = packet
                }
                return
            }


            pendingPackets[packet.id] = packet   // ✅ STORE

            log("[RECEIVED] ${packet.message}")
        }


        override fun onPayloadTransferUpdate(
            id: String,
            update: PayloadTransferUpdate
        ) {}
    }

    fun send(packet: Packet) {
        val now = System.currentTimeMillis()
        if (now > packet.expiredAt) return
        if (seenPackets.containsKey(packet.id)) return

        seenPackets[packet.id] = packet.expiredAt
        pendingPackets[packet.id] = packet

        // 🔑 Trigger immediate evaluation
        handler.post { evaluateSystemState() }
    }

    private fun isGateway(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager

        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false

        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun evaluateSystemState() {
        val now = System.currentTimeMillis()

        // 1. Drop expired packets
        pendingPackets.entries.removeIf { now > it.value.expiredAt }

        // 2. If not a gateway, nothing else to do
        if (!isGateway()) return

        // 3. Deliver all undelivered SOS packets
        pendingPackets.values
            .filter { !it.isAck }
            .toList()
            .forEach { sos ->
                if (ackedPackets.contains(sos.id)) return@forEach

                log("🌐 GATEWAY delivering ${sos.id}")

                // deliverToInternet(sos)

                createAndStoreAck(sos)
                ackedPackets.add(sos.id)
                pendingPackets.remove(sos.id)
            }
    }

    private fun createAndStoreAck(sos: Packet) {
        val now = System.currentTimeMillis()

        val ack = Packet(
            message = "ACK",
            priority = 0,
            expiredAt = now + 60_000L,
            lat = null,
            lng = null,
            sourceDevice = endpointName,            // THIS node
            originalSenderId = sos.sourceDevice,    // SOS origin
            sourceTimeMillis = now,
            isAck = true,
            targetPacketId = sos.id
        )

        seenPackets[ack.id] = ack.expiredAt
        pendingPackets[ack.id] = ack
    }


    private fun forward(packet: Packet) {
        val bytes = Gson().toJson(packet).toByteArray()
        val payload = Payload.fromBytes(bytes)

        peers.forEach {
            client.sendPayload(it, payload)
        }

        log("[FORWARD] ${packet.isAck}")
    }


    private val handler = Handler(Looper.getMainLooper())

    private val rebroadcastTask = object : Runnable {
        override fun run() {
            pendingPackets.values.forEach { forward(it) }
            evaluateSystemState()
            handler.postDelayed(this, 10_000L)
        }
    }


    private val cleanupTask = object : Runnable {
        override fun run() {
            val now = System.currentTimeMillis()
            seenPackets.entries.removeIf { now > it.value }
            handler.postDelayed(this, 60_000L)
        }
    }

    fun stop() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) { }
    }

}

