package com.example.yrmultimediaco.sos

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson

class MainActivity : AppCompatActivity() {

    lateinit var fusedLocationClient: FusedLocationProviderClient

    var currentLat: Double? = null
    var currentLng: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        logView = findViewById(R.id.txtLogs)

        val btnSOS = findViewById<Button>(R.id.btnSendSOS)
        val btnLow = findViewById<Button>(R.id.btnSendLow)

        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)


        if (!hasAllPermissions()) {
            requestNearbyPermissions()
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU && !isLocationEnabled()) {
            Toast.makeText(this, "Turn ON Location", Toast.LENGTH_LONG).show()
        } else {
            startMesh()
        }

        fetchLastLocation()

        btnSOS.setOnClickListener {
            val packet = createSOSPacket(
                lat = currentLat,
                lng = currentLng
            )

            meshManager.send(packet)

            logView.append("\n[SENT] SOS")
        }

        btnLow.setOnClickListener {
            val packet = createSOSPacket(
                lat = currentLat,
                lng = currentLng
            )

            meshManager.send(packet)

            logView.append("\n[SENT] LOW")
        }


    }

    fun createSOSPacket(
        lat: Double?,
        lng: Double?
    ): Packet {
        return Packet(
            type = PacketType.SOS,
            message = "SOS EMERGENCY",
            priority = 2,
            ttl = 2,
            lat = lat,
            lng = lng,
            sourceTimeMillis = System.currentTimeMillis()
        )
    }


    private val nearbyPermissions: Array<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.NEARBY_WIFI_DEVICES,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        }



    private fun hasAllPermissions(): Boolean {
        return nearbyPermissions.all {
            ContextCompat.checkSelfPermission(this, it) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestNearbyPermissions() {
        ActivityCompat.requestPermissions(
            this,
            nearbyPermissions,
            101
        )
    }


    private fun isLocationEnabled(): Boolean {
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 101 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            startMesh()
        } else {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                Toast.makeText(
                    this,
                    "Enable Location permission AND turn Location ON",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    this,
                    "Nearby device permission required",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    }

    lateinit var meshManager: MeshManager
    lateinit var logView: TextView

    private fun startMesh() {
        meshManager = MeshManager(this) { msg ->
            runOnUiThread {
                logView.append("\n$msg")
            }
        }

        meshManager.start()

        findViewById<Button>(R.id.btnSendSOS).isEnabled = true
        findViewById<Button>(R.id.btnSendLow).isEnabled = true
    }

    private fun fetchLastLocation() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    currentLat = location.latitude
                    currentLng = location.longitude

                    logView.append(
                        "\n[LOCATION] $currentLat , $currentLng"
                    )
                } else {
                    logView.append("\n[LOCATION] Not available")
                }
            }
            .addOnFailureListener {
                logView.append("\n[LOCATION] Failed")
            }
    }


}