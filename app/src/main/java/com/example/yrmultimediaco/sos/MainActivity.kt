package com.example.yrmultimediaco.sos

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.yrmultimediaco.sos.fragments.HomeFragment
import com.example.yrmultimediaco.sos.fragments.LogsFragment
import com.example.yrmultimediaco.sos.fragments.ProfileFragment
import com.example.yrmultimediaco.sos.fragments.SOSFragment
import com.example.yrmultimediaco.sos.fragments.StatusFragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson


class MainActivity : AppCompatActivity() {

    lateinit var meshManager: MeshManager
    lateinit var util: Util

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        util = Util()

        setupBottomNav()

        if (!hasAllPermissions()) {
            requestNearbyPermissions()
        } else if (!isLocationEnabled()) {
            Toast.makeText(this, "Turn ON Location", Toast.LENGTH_LONG).show()
        } else {
            startMesh()
        }
    }


    private fun setupBottomNav() {
        loadFragment(HomeFragment())

        findViewById<BottomNavigationView>(R.id.bottomNav)
            .setOnItemSelectedListener {
                when (it.itemId) {
                    R.id.home -> loadFragment(HomeFragment())
                    R.id.status -> loadFragment(StatusFragment())
                    R.id.sos -> loadFragment(SOSFragment())
                    R.id.logs -> loadFragment(LogsFragment())
                    R.id.profile -> loadFragment(ProfileFragment())
                }
                true
            }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }


    private fun startMesh() {
        if (::meshManager.isInitialized) return

        meshManager = MeshManager(this) { msg ->
            Log.d("MESH", msg)
        }

        meshManager.start()
        Log.d("MESH", "Mesh started from MainActivity")
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

    private fun hasAllPermissions(): Boolean =
        nearbyPermissions.all {
            ContextCompat.checkSelfPermission(this, it) ==
                    PackageManager.PERMISSION_GRANTED
        }

    private fun requestNearbyPermissions() {
        ActivityCompat.requestPermissions(this, nearbyPermissions, 101)
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
            Toast.makeText(
                this,
                "Permissions + Location required",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::meshManager.isInitialized) {
            meshManager.stop()
        }
    }
}


/*
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
            val packet = createLOWPacket()

            meshManager.send(packet)

            logView.append("\n[SENT] LOW")
        }
    }

    fun createSOSPacket(
        lat: Double?,
        lng: Double?
    ) : Packet {
        val time = System.currentTimeMillis()
        return Packet(
            type = PacketType.SOS,
            message = "SOS EMERGENCY",
            priority = Priority.SOS,
            expiredAt = time + util.ttlForPriority(Priority.SOS),
            lat = lat,
            lng = lng,
            sourceTimeMillis = time
        )
    }

    fun createLOWPacket() : Packet {
        val time = System.currentTimeMillis()
        return Packet(
            type = PacketType.LOW_STATUS,
            message = "LOW",
            priority = Priority.LOW,
            expiredAt = time + util.ttlForPriority(Priority.LOW),
            sourceTimeMillis = time
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
    lateinit var util: Util
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

    override fun onDestroy() {
        super.onDestroy()
        meshManager.stop()
    }

}*/
