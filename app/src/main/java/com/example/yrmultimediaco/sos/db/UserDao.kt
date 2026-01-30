package com.example.yrmultimediaco.sos.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

import com.example.yrmultimediaco.sos.data.UserProfileEntity

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(user: UserProfileEntity)

    @Query("SELECT * FROM user_profile LIMIT 1")
    suspend fun getProfile(): UserProfileEntity?
}
