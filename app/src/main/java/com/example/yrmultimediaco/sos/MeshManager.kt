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
import androidx.core.content.ContentProviderCompat.requireContext
import com.example.yrmultimediaco.sos.data.Packet
import com.example.yrmultimediaco.sos.data.Prefs
import java.util.PriorityQueue


class MeshManager(
    private val context: Context,
    private val log: (String) -> Unit
) {

    private val client = Nearby.getConnectionsClient(context)
    private val SERVICE_ID = "EMERGENCY_MESH_V1"
    private val MAX_PACKETS_PER_ROUND = 5
    private val peers = mutableSetOf<String>()
    private var running = false
    private val seenPackets = mutableMapOf<String, Long>() // id -> expiredAt
    private val pendingPackets = mutableMapOf<String, Packet>() // id -> packet
    private val ackedPackets = mutableSetOf<String>() // SOS ids already ACKed
    var onAckReceived: ((String?) -> Unit)? = null
    private val sendQueue = PriorityQueue<Packet>(
        compareBy<Packet> { it.priority }
            .thenBy { it.sourceTimeMillis }
    )
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

    val rssiTracker = BleRssiTracker()
    private val bleScanner = BleScanner(context, rssiTracker)

    fun start() {
        startAdvertising()
        startDiscovery()

        bleScanner.start()

        handler.post(rebroadcastTask)
        handler.post(cleanupTask)
        running = true
    }

    fun connectedCount(): Int {
        return peers.size;
    }

    fun isRunning(): Boolean {
        return running;
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

            // Deterministic initiator rule
            if (endpointName > info.endpointName) {
                log("Initiating connection to ${info.endpointName}")
                client.requestConnection(endpointName, id, lifecycleCallback)
            } else {
                log("Waiting for ${info.endpointName} to initiate")
            }
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
                    log("🎯 ORIGIN RECEIVED ACK — SOS SUCCESS ")
                    onAckReceived?.invoke(packet.targetPacketId)
                    pendingPackets.remove(packet.id)
                } else {
                    pendingPackets[packet.id] = packet
                    sendQueue.offer(packet)
                }
                return
            }

            pendingPackets[packet.id] = packet
            sendQueue.offer(packet)
            enforceQueueLimit()

            handler.post { evaluateSystemState() }

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

        handler.post { evaluateSystemState() }
        sendQueue.offer(packet)
        enforceQueueLimit()
    }

    public fun isGateway(): Boolean {
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
            .filter { it.priority != Priority.ACK }
            .toList()
            .forEach { sos ->
                if (ackedPackets.contains(sos.id)) return@forEach

                log("🌐 GATEWAY delivering ${sos.id}")

                // deliverToInternet(sos)
                if(sos.sourceDevice == endpointName) {
                    log("🎯 ORIGIN DELIVERED SOS")
                    onAckReceived?.invoke(sos.id)
                } else {
                    createAndStoreAck(sos)
                    ackedPackets.add(sos.id)
                }
                pendingPackets.remove(sos.id)
            }
    }

    private fun createAndStoreAck(sos: Packet) {
        val now = System.currentTimeMillis()
        val profile = Prefs(context).getUserProfile()

        val userId: String = profile?.userId ?: "dummy_user_id"

        val ack = Packet(
            message = "ACK",
            priority = Priority.ACK,
            expiredAt = now + Util.ttlForPriority(Priority.ACK),
            sourceDevice = endpointName,
            originalSenderId = sos.sourceDevice,
            sourceTimeMillis = now,
            isAck = true,
            payloadUserId = userId,
            targetPacketId = sos.id
        )

        seenPackets[ack.id] = ack.expiredAt
        pendingPackets[ack.id] = ack
        sendQueue.offer(ack)
    }

    private fun forward(packet: Packet) {
        val bytes = Gson().toJson(packet).toByteArray()
        val payload = Payload.fromBytes(bytes)

        peers.forEach {
            client.sendPayload(it, payload)
        }

        log("[FORWARD] ${packet.isAck}")
    }

    private fun enforceQueueLimit() {
        val MAX_QUEUE_SIZE = 50
        if (sendQueue.size <= MAX_QUEUE_SIZE) return

        val iterator = sendQueue.iterator()
        while (sendQueue.size > MAX_QUEUE_SIZE && iterator.hasNext()) {
            val pkt = iterator.next()
            if (pkt.priority == Priority.LOW) {
                iterator.remove()
                pendingPackets.remove(pkt.id)
                log("🗑️ Dropped LOW priority packet ${pkt.id}")
            }
        }
    }

    private val handler = Handler(Looper.getMainLooper())

    private val rebroadcastTask = object : Runnable {
        override fun run() {
            val now = System.currentTimeMillis()
            val toSend = mutableListOf<Packet>()

            // Pull highest priority packets first
            while (sendQueue.isNotEmpty() && toSend.size < MAX_PACKETS_PER_ROUND) {
                val pkt = sendQueue.poll()

                if (!pendingPackets.containsKey(pkt.id)) continue
                if (now > pkt.expiredAt) {
                    pendingPackets.remove(pkt.id)
                    continue
                }

                toSend.add(pkt)
            }

            // Send them
            toSend.forEach { forward(it) }

            // Reinsert for epidemic rebroadcast
            toSend.forEach { sendQueue.offer(it) }

            // Evaluate gateway after sending
            evaluateSystemState()

            handler.postDelayed(this, nextRebroadcastDelay())
        }
    }

    private fun nextRebroadcastDelay(): Long {
        if (pendingPackets.isEmpty()) return 10_000L

        val now = System.currentTimeMillis()
        val youngest = pendingPackets.values.minOf { now - it.sourceTimeMillis }

        return when {
            youngest < 60_000L -> 10_000L
            youngest < 5 * 60_000L -> 30_000L
            else -> 60_000L
        }
    }

    private val cleanupTask = object : Runnable {
        override fun run() {
            val now = System.currentTimeMillis()
            seenPackets.entries.removeIf { now > it.value }
            handler.postDelayed(this, 10_000L)
        }
    }

    fun stop() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            bleScanner.stop()
            running = false
        } catch (e: Exception) { }
    }


    fun getDistance(endpointId: String): DistanceEstimate? {
        val exactMatch = rssiTracker.getDistance(endpointId)
        if (exactMatch != null) return exactMatch

        val bestGuessId = rssiTracker.getMostLikelyDevice() ?: return null
        return rssiTracker.getDistance(bestGuessId)
    }

    fun getLinkQuality(endpointId: String): LinkQuality? {
        val exactMatch = rssiTracker.getQuality(endpointId)
        if (exactMatch != null) return exactMatch

        val bestGuessId = rssiTracker.getMostLikelyDevice() ?: return null
        return rssiTracker.getQuality(bestGuessId)
    }

    fun connectedEndpoints(): Set<String> = peers.toSet()

}

