package com.example.yrmultimediaco.sos.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.yrmultimediaco.sos.MainActivity
import com.example.yrmultimediaco.sos.R
import com.example.yrmultimediaco.sos.data.UserProfileEntity
import com.example.yrmultimediaco.sos.db.AppDatabase
import com.example.yrmultimediaco.sos.viewModels.ProfileViewModel
import kotlinx.coroutines.launch

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private lateinit var edtName: EditText
    private lateinit var edtPhone: EditText
    private lateinit var edtBlood: EditText
    private lateinit var edtAssist: EditText
    private lateinit var edtEmerName: EditText
    private lateinit var edtEmerPhone: EditText

    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    private var originalProfile: UserProfileEntity? = null
    private var isDirty = false
    private lateinit var actionLayout: View
    private lateinit var btnEdit: ImageButton

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        btnEdit = view.findViewById(R.id.btnEdit)
//        actionLayout = view.findViewById(R.id.actionLayout)

        bindViews(view)
        fetchProfile()
        setupListeners()

//        btnEdit.setOnClickListener {
//            enableEditMode(true)
//        }
    }

    private fun bindViews(view: View) {
        edtName = view.findViewById(R.id.edtName)
        edtPhone = view.findViewById(R.id.edtPhone)
        edtBlood = view.findViewById(R.id.edtBloodGroup)
        edtAssist = view.findViewById(R.id.edtAssistance)
        edtEmerName = view.findViewById(R.id.edtEmergencyName)
        edtEmerPhone = view.findViewById(R.id.edtEmergencyPhone)

        btnSave = view.findViewById(R.id.btnSave)
        btnCancel = view.findViewById(R.id.btnCancel)
    }

    private fun fetchProfile() {
        lifecycleScope.launch {
            val profile = AppDatabase
                .getInstance(requireContext())
                .userDao()
                .getProfile()

            profile?.let {
                originalProfile = it
                fillData(it)
            }
        }
    }

    private fun fillData(profile: UserProfileEntity) {
        edtName.setText(profile.name)
        edtPhone.setText(profile.phoneNumber)
        edtBlood.setText(profile.bloodGroup)
        edtAssist.setText(profile.specialAssistance)
        edtEmerName.setText(profile.emergencyContactName)
        edtEmerPhone.setText(profile.emergencyContactNumber)
    }

    private fun setupListeners() {

        val fields = listOf(
            edtName, edtPhone, edtBlood,
            edtAssist, edtEmerName, edtEmerPhone
        )

        fields.forEach { editText ->
            editText.setOnClickListener {
                enableEditMode()
            }

            editText.addTextChangedListener {
                markDirty()
            }
        }

        btnSave.setOnClickListener {
            saveProfile()
        }

        btnCancel.setOnClickListener {
            originalProfile?.let {
                fillData(it)
            }
            disableEditMode()
        }
    }

    private fun enableEditMode() {
        listOf(
            edtName, edtPhone, edtBlood,
            edtAssist, edtEmerName, edtEmerPhone
        ).forEach { it.isEnabled = true }
    }

    private fun disableEditMode() {
        listOf(
            edtName, edtPhone, edtBlood,
            edtAssist, edtEmerName, edtEmerPhone
        ).forEach { it.isEnabled = false }

        btnSave.isEnabled = false
        btnCancel.isEnabled = false
        isDirty = false
    }

    private fun markDirty() {
        isDirty = true
        btnSave.isEnabled = true
        btnCancel.isEnabled = true

        (requireActivity() as? MainActivity)
            ?.setUnsavedChanges(true)
    }

    fun saveProfile() {
        val updated = UserProfileEntity(
           // userId = originalProfile?.userId ?: 0,
            name = edtName.text.toString(),
            phoneNumber = edtPhone.text.toString(),
            bloodGroup = edtBlood.text.toString(),
            specialAssistance = edtAssist.text.toString(),
            emergencyContactName = edtEmerName.text.toString(),
            emergencyContactNumber = edtEmerPhone.text.toString()
        )

        lifecycleScope.launch {
            AppDatabase.getInstance(requireContext())
                .userDao()
                .insertOrUpdate(updated)

            originalProfile = updated
            disableEditMode()

            (requireActivity() as? MainActivity)
                ?.setUnsavedChanges(false)
        }
    }
}
