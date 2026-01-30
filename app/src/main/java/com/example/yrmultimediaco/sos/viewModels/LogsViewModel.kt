package com.example.yrmultimediaco.sos.viewModels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.text.SimpleDateFormat
import java.util.*

class LogsViewModel : ViewModel() {

    val logs = MutableLiveData<List<String>>(emptyList())

    private val buffer = mutableListOf<String>()
    private val formatter =
        SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun append(message: String) {
        val timestamped =
            "[${formatter.format(Date())}] $message"

        buffer.add(0, timestamped) // newest on top
        logs.postValue(buffer.toList())
    }

    fun clear() {
        buffer.clear()
        logs.postValue(emptyList())
    }
}
