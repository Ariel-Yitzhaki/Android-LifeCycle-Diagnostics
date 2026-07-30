package com.ariel.lifecycle.sampleviews.fragments

import androidx.lifecycle.ViewModel
import com.ariel.lifecycle.sampleviews.core.GlobalListenerRegistry

/**
 * FAULT: registers itself with a process-lifetime singleton and never unregisters.
 *
 * `onCleared` is where that would happen, and it is not overridden. Every fragment instance that
 * asks for this ViewModel adds one more permanently-reachable object.
 */
class LeakyRegistryViewModel : ViewModel() {
    init {
        GlobalListenerRegistry.register(this)
    }
}

/** CONTROL: same registration, undone in `onCleared`. */
class CleanRegistryViewModel : ViewModel() {
    init {
        GlobalListenerRegistry.register(this)
    }

    override fun onCleared() {
        GlobalListenerRegistry.unregister(this)
        super.onCleared()
    }
}
