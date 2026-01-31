package com.example.yrmultimediaco.sos

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.example.yrmultimediaco.sos.data.Prefs
import com.example.yrmultimediaco.sos.data.UserProfileEntity
import com.example.yrmultimediaco.sos.db.AppDatabase
import kotlinx.coroutines.launch

class OnBoardingActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_onboarding)

        prefs = Prefs(this)

        val btnSave = findViewById<Button>(R.id.btnSaveProfile)

        val edtName = findViewById<EditText>(R.id.edtName)
        val edtPhone = findViewById<EditText>(R.id.edtPhone)
        val edtBlood = findViewById<EditText>(R.id.edtBloodGroup)
        val edtEmergencyName = findViewById<EditText>(R.id.edtEmergencyName)
        val edtEmergencyPhone = findViewById<EditText>(R.id.edtEmergencyPhone)
        val edtAssist = findViewById<EditText>(R.id.edtSpecialAssistance)

        val fields = listOf(
            edtName,
            edtPhone,
            edtBlood,
            edtEmergencyName,
            edtEmergencyPhone
        )

        btnSave.isEnabled = false

        fields.forEach { editText ->
            editText.addTextChangedListener {
                btnSave.isEnabled = fields.all { it.text.isNotBlank() }
            }
        }

        btnSave.setOnClickListener {

            val profile = UserProfileEntity (
                name = edtName.text.toString(),
                phoneNumber = edtPhone.text.toString(),
                bloodGroup = edtBlood.text.toString(),
                specialAssistance = edtAssist.text.toString(),
                emergencyContactName = edtEmergencyName.text.toString(),
                emergencyContactNumber = edtEmergencyPhone.text.toString()
            )

            saveProfile(profile)
        }
    }

    private fun saveProfile(profile: UserProfileEntity) {
        lifecycleScope.launch {
            AppDatabase.getInstance(applicationContext)
                .userDao()
                .insertOrUpdate(profile)

            prefs.markProfileCompleted()

            startActivity(
                Intent(this@OnBoardingActivity, MainActivity::class.java)
            )
            finish()
        }
    }

    // 🚫 Disable back press until saved
    override fun onBackPressed() {
        Toast.makeText(
            this,
            "Please complete onboarding",
            Toast.LENGTH_SHORT
        ).show()
    }
}