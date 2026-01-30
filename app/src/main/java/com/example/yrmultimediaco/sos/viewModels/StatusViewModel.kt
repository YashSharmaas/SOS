package com.example.yrmultimediaco.sos.viewModels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
class StatusViewModel : ViewModel() {

    val selectedType = MutableLiveData<Int>() // radio id
    val otherText = MutableLiveData<String>()

    val isSending = MutableLiveData(false)
    val statusText = MutableLiveData<String>()

    var lastPacketId: String? = null

    fun handleAck(id: String?) {
        if (id == lastPacketId) {
            isSending.postValue(false)
            statusText.postValue("✅ Status delivered")
        }
    }
}