package com.example.yrmultimediaco.sos.util

import com.example.yrmultimediaco.sos.viewModels.LogsViewModel

/**
 * Simple in-memory log event bridge.
 * Allows non-UI layers to emit logs safely.
 */
object LogsBus {

    private var viewModel: LogsViewModel? = null

    /**
     * Called by LogsFragment when UI becomes active.
     */
    fun attach(vm: LogsViewModel) {
        viewModel = vm
    }

    /**
     * Called from anywhere (mesh, gateway, scheduler).
     */
    fun emit(message: String) {
        viewModel?.append(message)
    }
}
