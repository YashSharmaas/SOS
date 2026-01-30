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
import android.widget.RadioGroup
import android.widget.EditText
import com.example.yrmultimediaco.sos.viewModels.StatusViewModel
import androidx.fragment.app.activityViewModels
import androidx.core.widget.doAfterTextChanged
import com.example.yrmultimediaco.sos.util.Logger

class StatusFragment : Fragment(R.layout.fragment_status) {

    private val statusVM: StatusViewModel by activityViewModels()
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

        meshManager = (requireActivity() as MainActivity).meshManager
        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(requireContext())

        radioGroup = view.findViewById(R.id.radioLowGroup)
        otherField = view.findViewById(R.id.etOther)
        sendBtn = view.findViewById(R.id.btnSendLOW)
        statusText = view.findViewById(R.id.txtStatus)

        statusVM.selectedType.observe(viewLifecycleOwner) {
            if (it != null) radioGroup.check(it)
        }

        statusVM.otherText.observe(viewLifecycleOwner) {
            otherField.setText(it ?: "")
        }

        statusVM.statusText.observe(viewLifecycleOwner) {
            statusText.text = it ?: ""
        }

        statusVM.isSending.observe(viewLifecycleOwner) {
            sendBtn.isEnabled = !(it ?: false)
        }


        fetchLastLocation()

        radioGroup.setOnCheckedChangeListener { _, id ->
            statusVM.selectedType.value = id
            otherField.visibility =
                if (id == R.id.rbOther) View.VISIBLE else View.GONE
        }

        otherField.doAfterTextChanged {
            statusVM.otherText.value = it.toString()
        }

        meshManager.onAckReceived = { id ->
            statusVM.handleAck(id)
        }

        sendBtn.setOnClickListener {

            val message = buildLOWMessage() ?: return@setOnClickListener

            val packet = createLOWPacket(message)
            statusVM.lastPacketId = packet.id

            meshManager.send(packet)

            statusVM.isSending.value = true
            statusVM.statusText.value = "📡 Hopping through mesh..."
            Logger.mesh("[SENT] STATUS")
        }
    }

    private fun buildLOWMessage(): String? {
        return when (radioGroup.checkedRadioButtonId) {
            R.id.rbSafe -> "LOW: I AM SAFE"
            R.id.rbFood -> "LOW: NEED FOOD AND WATER"
            R.id.rbClothes -> "LOW: CLOTHES AND ESSENTIALS"
            R.id.rbRelocation -> "LOW: RELOCATION NEEDED"
            R.id.rbOther -> {
                val text = otherField.text.toString()
                if (text.isBlank()) null else "LOW: $text"
            }
            else -> null
        }
    }

    private fun createLOWPacket(message: String): Packet {
        val time = System.currentTimeMillis()

        return Packet(
            type = PacketType.LOW_STATUS,
            message = message,
            priority = Priority.LOW,
            expiredAt = time + Util.ttlForPriority(Priority.LOW),
            lat = currentLat,
            lng = currentLng,
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