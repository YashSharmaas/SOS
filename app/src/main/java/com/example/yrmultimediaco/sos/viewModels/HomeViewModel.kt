package com.example.yrmultimediaco.sos.viewModels

import android.bluetooth.BluetoothAdapter
import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.yrmultimediaco.sos.MeshManager
import android.net.ConnectivityManager
import android.os.BatteryManager

class HomeViewModel(
    private val context: Context,
    private val meshManager: MeshManager
) : ViewModel() {

    val bluetoothOn = MutableLiveData<Boolean>()
    val meshRunning = MutableLiveData<Boolean>()
    val endpointCount = MutableLiveData<Int>()
    val internetAvailable = MutableLiveData<Boolean>()
    val gatewayMode = MutableLiveData<Boolean>()
    val batteryPercent = MutableLiveData<Int>()

    fun refreshAll() {
        bluetoothOn.value = isBluetoothOn()
        meshRunning.value = meshManager.isRunning()
        endpointCount.value = meshManager.connectedCount()
        internetAvailable.value = hasInternet()
        gatewayMode.value = meshManager.isGateway()
        batteryPercent.value = readBattery()
    }

    private fun isBluetoothOn(): Boolean {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        return adapter?.isEnabled == true
    }

    private fun hasInternet(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager
        return cm.activeNetworkInfo?.isConnected == true
    }

    private fun readBattery(): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE)
                as BatteryManager
        return bm.getIntProperty(
            BatteryManager.BATTERY_PROPERTY_CAPACITY
        )
    }
}
