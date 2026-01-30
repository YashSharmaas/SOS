package com.example.yrmultimediaco.sos.ui

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.yrmultimediaco.sos.R
import com.example.yrmultimediaco.sos.data.Prefs
import com.example.yrmultimediaco.sos.data.UserProfileEntity
import com.example.yrmultimediaco.sos.db.AppDatabase
import kotlinx.coroutines.launch

class OnboardingFragment : Fragment(R.layout.fragment_onboarding) {

    var isDirty = false
    private lateinit var prefs: Prefs

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        prefs = Prefs(requireContext())

        val btnSave = view.findViewById<Button>(R.id.btnSaveProfile)

        val edtName = view.findViewById<EditText>(R.id.edtName)
        val edtPhone = view.findViewById<EditText>(R.id.edtPhone)
        val edtBlood = view.findViewById<EditText>(R.id.edtBloodGroup)
        val edtEmerName = view.findViewById<EditText>(R.id.edtEmergencyName)
        val edtEmerPhone = view.findViewById<EditText>(R.id.edtEmergencyPhone)

        val fields = listOf(
            edtName, edtPhone, edtBlood, edtEmerName, edtEmerPhone
        )

        fields.forEach {
            it.addTextChangedListener {
                isDirty = true
                btnSave.isEnabled = fields.all { f -> f.text.isNotBlank() }
            }
        }

        fields.forEach { it.addTextChangedListener { isDirty = true } }

        btnSave.setOnClickListener {
            if (validateFields(fields)) {
                val profile = buildUserProfile(view)
                saveToRoom(profile)
                prefs.markProfileCompleted()
                isDirty = false
                navigateToHome()
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (isDirty) {
                requireActivity().showUnsavedDialog(
                    onSave = { btnSave.performClick() },
                    onDiscard = { requireActivity().finish() }
                )
            } else {
                requireActivity().finish()
            }
        }
    }

    private fun navigateToHome() {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, HomeFragment())
            .commit()
    }

    fun Activity.showUnsavedDialog(
        onSave: () -> Unit,
        onDiscard: () -> Unit
    ) {
        AlertDialog.Builder(this)
            .setTitle("Unsaved changes")
            .setMessage("You have unsaved changes. Save them?")
            .setPositiveButton("Save") { _, _ -> onSave() }
            .setNegativeButton("Discard") { _, _ -> onDiscard() }
            .setCancelable(true)
            .show()
    }

    private fun saveToRoom(profile: UserProfileEntity) {
        lifecycleScope.launch {
            AppDatabase.getInstance(requireContext())
                .userDao()
                .insertOrUpdate(profile)
        }
    }

    private fun buildUserProfile(view: View): UserProfileEntity {
        return UserProfileEntity(
            name = view.findViewById<EditText>(R.id.edtName).text.toString(),
            phoneNumber = view.findViewById<EditText>(R.id.edtPhone).text.toString(),
            bloodGroup = view.findViewById<EditText>(R.id.edtBloodGroup).text.toString(),
            specialAssistance = view.findViewById<EditText>(R.id.edtSpecialAssistance)
                .text.toString().takeIf { it.isNotBlank() },
            emergencyContactName =
                view.findViewById<EditText>(R.id.edtEmergencyName).text.toString(),
            emergencyContactNumber =
                view.findViewById<EditText>(R.id.edtEmergencyPhone).text.toString()
        )
    }

    private fun validateFields(fields: List<EditText>): Boolean {
        var isValid = true

        for (field in fields) {
            if (field.text.toString().trim().isEmpty()) {
                field.error = "This field is required"
                if (isValid) field.requestFocus()
                isValid = false
            }
        }

        return isValid
    }

}
