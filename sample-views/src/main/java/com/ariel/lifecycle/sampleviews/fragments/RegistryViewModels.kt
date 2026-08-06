package com.ariel.lifecycle.sampleviews.fragments

import androidx.lifecycle.ViewModel
import com.ariel.lifecycle.sampleviews.core.GlobalListenerRegistry

/**
 * FAULT: registers itself with a process-lifetime singleton and never unregisters, and holds a
 * callback into the screen that created it.
 *
 * Either half on its own is survivable. Together they are the leak that matters: the registry keeps
 * the ViewModel for the life of the process, the ViewModel keeps [onUpdate], and [onUpdate] is a
 * lambda written inside a Fragment, so it keeps that Fragment and everything it reaches.
 *
 * `onCleared` is where both would be undone, and it is not overridden.
 */
class LeakyRegistryViewModel : ViewModel() {

    /**
     * Set by the fragment so the ViewModel can ask it to redraw. Whatever is put here captures the
     * fragment that wrote it, and nothing ever sets it back to null.
     */
    var onUpdate: (() -> Unit)? = null

    init {
        GlobalListenerRegistry.register(this)
    }
}

/** CONTROL: the same registration and the same callback, both dropped in `onCleared`. */
class CleanRegistryViewModel : ViewModel() {

    var onUpdate: (() -> Unit)? = null

    init {
        GlobalListenerRegistry.register(this)
    }

    override fun onCleared() {
        onUpdate = null
        GlobalListenerRegistry.unregister(this)
        super.onCleared()
    }
}
