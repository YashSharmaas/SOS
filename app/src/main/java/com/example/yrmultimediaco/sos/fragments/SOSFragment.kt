package com.example.yrmultimediaco.sos.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.example.yrmultimediaco.sos.MainActivity
import com.example.yrmultimediaco.sos.MeshManager
import com.example.yrmultimediaco.sos.Packet
import com.example.yrmultimediaco.sos.PacketType
import com.example.yrmultimediaco.sos.Priority
import com.example.yrmultimediaco.sos.R
import com.example.yrmultimediaco.sos.Util
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class SOSFragment : Fragment(R.layout.fragment_s_o_s) {

    private lateinit var meshManager: MeshManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var currentLat: Double? = null
    private var currentLng: Double? = null

    private lateinit var logView: TextView
    private lateinit var util: Util

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logView = view.findViewById(R.id.txtLogs)
        util = Util()

        meshManager = (requireActivity() as MainActivity).meshManager

        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(requireContext())

        fetchLastLocation()

        view.findViewById<Button>(R.id.btnSendSOS).setOnClickListener {
            val packet = createSOSPacket()
            meshManager.send(packet)
            logView.append("\n[SENT] SOS")
        }

        view.findViewById<Button>(R.id.btnSendLow).setOnClickListener {
            val packet = createLowPacket()
            meshManager.send(packet)
            logView.append("\n[SENT] LOW")
        }
    }


    private fun createSOSPacket(): Packet {
        val time = System.currentTimeMillis()
        return Packet(
            type = PacketType.SOS,
            message = "SOS EMERGENCY",
            priority = Priority.SOS,
            expiredAt = time + util.ttlForPriority(Priority.SOS),
            lat = currentLat,
            lng = currentLng,
            sourceTimeMillis = time
        )
    }

    private fun createLowPacket(): Packet {
        val time = System.currentTimeMillis()
        return Packet(
            type = PacketType.LOW_STATUS,
            message = "LOW STATUS",
            priority = Priority.LOW,
            expiredAt = time + util.ttlForPriority(Priority.LOW),
            sourceTimeMillis = time
        )
    }


    private fun fetchLastLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    currentLat = location.latitude
                    currentLng = location.longitude
                    logView.append("\n[LOCATION] $currentLat , $currentLng")
                } else {
                    logView.append("\n[LOCATION] Not available")
                }
            }
    }
}

