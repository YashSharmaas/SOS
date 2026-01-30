package com.example.yrmultimediaco.sos.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.TextView
import com.example.yrmultimediaco.sos.MainActivity
import com.example.yrmultimediaco.sos.R
import com.example.yrmultimediaco.sos.viewModels.HomeViewModel

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var vm: HomeViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mesh = (requireActivity() as MainActivity).meshManager

        vm = HomeViewModel(requireContext(), mesh)

        val txtBluetooth = view.findViewById<TextView>(R.id.txtBluetooth)
        val txtMesh = view.findViewById<TextView>(R.id.txtMesh)
        val txtEndpoints = view.findViewById<TextView>(R.id.txtEndpoints)
        val txtGateway = view.findViewById<TextView>(R.id.txtGateway)
        val txtInternet = view.findViewById<TextView>(R.id.txtInternet)
        val txtBattery = view.findViewById<TextView>(R.id.txtBattery)

        vm.bluetoothOn.observe(viewLifecycleOwner) {
            txtBluetooth.text =
                "Bluetooth: " + if (it) "🟢 ON" else "🔴 OFF"
        }

        vm.meshRunning.observe(viewLifecycleOwner) {
            txtMesh.text =
                "Mesh: " + if (it) "🟢 ACTIVE" else "🔴 STOPPED"
        }

        vm.endpointCount.observe(viewLifecycleOwner) {
            txtEndpoints.text = "Endpoints: $it"
        }

        vm.gatewayMode.observe(viewLifecycleOwner) {
            txtGateway.text =
                if (it) "Gateway: 🌐 ENABLED"
                else "Gateway: Relay Only"
        }

        vm.internetAvailable.observe(viewLifecycleOwner) {
            txtInternet.text =
                if (it) "Internet: 🟢 Available"
                else "Internet: 🔴 Offline"
        }

        vm.batteryPercent.observe(viewLifecycleOwner) {
            txtBattery.text =
                "Battery: $it% " + batteryLabel(it)
        }

        vm.refreshAll()
    }

    private fun batteryLabel(p: Int): String =
        when {
            p < 20 -> "🔴 Critical"
            p < 40 -> "🟡 Low"
            else -> "🟢 Healthy"
        }
}
