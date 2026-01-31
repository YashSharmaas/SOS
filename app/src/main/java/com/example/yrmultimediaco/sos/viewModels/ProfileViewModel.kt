package com.example.yrmultimediaco.sos.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.yrmultimediaco.sos.data.UserProfileEntity
import com.example.yrmultimediaco.sos.db.AppDatabase
import kotlinx.coroutines.launch

class ProfileViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val dao =
        AppDatabase.getInstance(application).userDao()

    private var originalProfile: UserProfileEntity? = null

    private val _profile = MutableLiveData<UserProfileEntity?>()
    val profile: LiveData<UserProfileEntity?> = _profile

    private val _hasUnsavedChanges = MutableLiveData(false)
    val hasUnsavedChanges: LiveData<Boolean> = _hasUnsavedChanges

    fun loadProfile() {
        viewModelScope.launch {
            originalProfile = dao.getProfile()
            _profile.postValue(originalProfile)
            _hasUnsavedChanges.postValue(false)
        }
    }

    fun onFieldChanged(updated: UserProfileEntity) {
        _profile.value = updated
        _hasUnsavedChanges.value = updated != originalProfile
    }

    fun save() {
        viewModelScope.launch {
            _profile.value?.let {
                dao.insertOrUpdate(it)
                originalProfile = it
                _hasUnsavedChanges.postValue(false)
            }
        }
    }

    fun revert() {
        _profile.value = originalProfile
        _hasUnsavedChanges.value = false
    }
}
