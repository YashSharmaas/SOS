package com.example.yrmultimediaco.sos.data

import android.content.Context
import com.google.gson.Gson

class Prefs(context: Context) {

    private val sp =
        context.getSharedPreferences("sos_prefs", Context.MODE_PRIVATE)

    fun isProfileCompleted(): Boolean =
        sp.getBoolean("profile_done", false)

    fun markProfileCompleted() {
        sp.edit().putBoolean("profile_done", true).apply()
    }

    fun saveUserProfile(profile: UserProfileEntity) {
        sp.edit()
            .putString("user_profile", Gson().toJson(profile))
            .apply()
    }

    fun getUserProfile(): UserProfileEntity? {
        val json = sp.getString("user_profile", null) ?: return null
        return Gson().fromJson(json, UserProfileEntity::class.java)
    }
}
