package com.example.yrmultimediaco.sos

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context

class BleScanner(
    private val context: Context,
    private val rssiTracker: BleRssiTracker
) {

    private val scanner =
        BluetoothAdapter.getDefaultAdapter().bluetoothLeScanner

    private val scanCallback = object : ScanCallback() {

        override fun onScanResult(
            callbackType: Int,
            result: ScanResult
        ) {
            val device = result.device ?: return
            val rssi = result.rssi
            val deviceId = device.address   // Stable ID

            rssiTracker.addSample(deviceId, rssi)
        }
    }

    fun start() {
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner.startScan(null, settings, scanCallback)
    }

    fun stop() {
        scanner.stopScan(scanCallback)
    }
}
