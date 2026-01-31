package com.example.yrmultimediaco.sos.ui

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
import com.example.yrmultimediaco.sos.data.Packet
import com.example.yrmultimediaco.sos.PacketType
import com.example.yrmultimediaco.sos.Priority
import com.example.yrmultimediaco.sos.R
import com.example.yrmultimediaco.sos.Util
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.widget.RadioGroup
import android.widget.EditText
import android.widget.Toast
import com.example.yrmultimediaco.sos.viewModels.SosViewModel
import androidx.fragment.app.activityViewModels
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.example.yrmultimediaco.sos.data.Prefs
import com.example.yrmultimediaco.sos.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SOSFragment : Fragment(R.layout.fragment_s_o_s) {

    private val sosVM: SosViewModel by activityViewModels()
    private lateinit var meshManager: MeshManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var currentLat: Double? = null
    private var currentLng: Double? = null

    private lateinit var radioGroup: RadioGroup
    private lateinit var otherField: EditText
    private lateinit var sendBtn: Button
    private lateinit var statusText: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = activity as? MainActivity
        val meshManager = activity?.getMeshManagerSafely()

        if (meshManager == null) {
            Toast.makeText(
                requireContext(),
                "Mesh is initializing, please wait…",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(requireContext())

        radioGroup = view.findViewById(R.id.radioSOSGroup)
        otherField = view.findViewById(R.id.etOther)
        sendBtn = view.findViewById(R.id.btnSendSOS)
        statusText = view.findViewById(R.id.txtStatus)

        sosVM.selectedType.observe(viewLifecycleOwner) {
            if (it != null) radioGroup.check(it)
        }

        meshManager.onAckReceived = { id ->
            sosVM.handleAck(id)
        }

//        sosVM.otherText.observe(viewLifecycleOwner) {
//            otherField.setText(it ?: "")
//        }
        otherField.doAfterTextChanged {
            sosVM.otherText.value = it?.toString()
        }

        sosVM.statusText.observe(viewLifecycleOwner) {
            statusText.text = it ?: ""
        }

        sosVM.isSending.observe(viewLifecycleOwner) {
            sendBtn.isEnabled = !(it ?: false)
        }


        fetchLastLocation()

        radioGroup.setOnCheckedChangeListener { _, id ->
            sosVM.selectedType.value = id

            if (id == R.id.rbOther) {
                otherField.visibility = View.VISIBLE
            } else {
                otherField.visibility = View.GONE
                otherField.text?.clear()
                sosVM.otherText.value = null
            }
        }

        otherField.doAfterTextChanged {
            sosVM.otherText.value = it.toString()
        }

        sendBtn.setOnClickListener {
            val activity = requireActivity() as MainActivity
            // Check if initialized before using
            val message = buildSOSMessage() ?: return@setOnClickListener
            //val packet = createSOSPacket(message)

            sendBtn.isEnabled = false

            viewLifecycleOwner.lifecycleScope.launch {
                val packet = withContext(Dispatchers.Default) {
                    createSOSPacket(message)
                }
                activity.meshManager.send(packet) // Access it here safely

                sosVM.lastPacketId = packet.id
                sosVM.isSending.postValue(true)
                sosVM.statusText.postValue("📡 Hopping through mesh...")
                Logger.mesh("[SENT] SOS")
            }
        }
//        sendBtn.setOnClickListener {
//
//            val message = buildSOSMessage() ?: return@setOnClickListener
//
//            val packet = createSOSPacket(message)
//            sosVM.lastPacketId = packet.id
//
//            meshManager.send(packet)
//
//            sosVM.isSending.value = true
//            sosVM.statusText.value = "📡 Hopping through mesh..."
//            Logger.mesh("[SENT] SOS")
//        }
    }

    private fun buildSOSMessage(): String? {
        return when (radioGroup.checkedRadioButtonId) {
            R.id.rbMedical -> "SOS: MEDICAL"
            R.id.rbKidnapping -> "SOS: KIDNAPPING"
            R.id.rbFire -> "SOS: FIRE"
            R.id.rbCollapse -> "SOS: BUILDING COLLAPSE"
            R.id.rbRiot -> "SOS: RIOT/TERROR"
            R.id.rbOther -> {
                val text = otherField.text.toString()
                if (text.isBlank()) null else "SOS: $text"
            }
            else -> null
        }
    }

    private fun createSOSPacket(message: String): Packet {
        val time = System.currentTimeMillis()
        val profile = Prefs(requireContext()).getUserProfile()
        val userId: String = profile?.userId ?: "dummy_user_id"

        return Packet(
            type = PacketType.SOS,
            message = message,
            priority = Priority.SOS,
            expiredAt = time + Util.ttlForPriority(Priority.SOS),
            lat = currentLat,
            lng = currentLng,
            payloadUserId = userId,
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

                    Logger.location(
                        "$currentLat , $currentLng"
                    )
                } else {
                    Logger.location("Not available")
                }
            }
            .addOnFailureListener {
                Logger.location("Failed")
            }
    }
}


//class SOSFragment : Fragment(R.layout.fragment_s_o_s) {
//
//    private lateinit var meshManager: MeshManager
//    private lateinit var fusedLocationClient: FusedLocationProviderClient
//
//    private var currentLat: Double? = null
//    private var currentLng: Double? = null
//
//    private lateinit var logView: TextView
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        logView = view.findViewById(R.id.txtLogs)
//
//        meshManager = (requireActivity() as MainActivity).meshManager
//
//        fusedLocationClient =
//            LocationServices.getFusedLocationProviderClient(requireContext())
//
//        fetchLastLocation()
//
//        view.findViewById<Button>(R.id.btnSendSOS).setOnClickListener {
//            val packet = createSOSPacket()
//            meshManager.send(packet)
//            logView.append("\n[SENT] SOS")
//        }
//
//        view.findViewById<Button>(R.id.btnSendLow).setOnClickListener {
//            val packet = createLowPacket()
//            meshManager.send(packet)
//            logView.append("\n[SENT] LOW")
//        }
//    }
//
//
//    private fun createSOSPacket(): Packet {
//        val time = System.currentTimeMillis()
//        return Packet(
//            type = PacketType.SOS,
//            message = "SOS EMERGENCY",
//            priority = Priority.SOS,
//            expiredAt = time + Util.ttlForPriority(Priority.SOS),
//            lat = currentLat,
//            lng = currentLng,
//            sourceTimeMillis = time
//        )
//    }
//
//    private fun createLowPacket(): Packet {
//        val time = System.currentTimeMillis()
//        return Packet(
//            type = PacketType.LOW_STATUS,
//            message = "LOW STATUS",
//            priority = Priority.LOW,
//            expiredAt = time + Util.ttlForPriority(Priority.LOW),
//            sourceTimeMillis = time
//        )
//    }
//
//    private fun fetchLastLocation() {
//        if (ActivityCompat.checkSelfPermission(
//                requireContext(),
//                Manifest.permission.ACCESS_FINE_LOCATION
//            ) != PackageManager.PERMISSION_GRANTED
//        ) return
//
//        fusedLocationClient.lastLocation
//            .addOnSuccessListener { location ->
//                if (location != null) {
//                    currentLat = location.latitude
//                    currentLng = location.longitude
//                    logView.append("\n[LOCATION] $currentLat , $currentLng")
//                } else {
//                    logView.append("\n[LOCATION] Not available")
//                }
//            }
//    }
//}

