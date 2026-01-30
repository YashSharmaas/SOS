package com.example.yrmultimediaco.sos.data

import androidx.room.Entity
import java.util.UUID

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    val userId: String = UUID.randomUUID().toString(),

    val name: String,
    val phoneNumber: String,
    val bloodGroup: String,

    val specialAssistance: String?, // optional

    val emergencyContactName: String,
    val emergencyContactNumber: String,

    val lastUpdated: Long = System.currentTimeMillis()
)

