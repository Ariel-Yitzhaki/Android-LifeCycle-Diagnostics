package com.ariel.lifecycle.samplecompose.core

import java.util.concurrent.CopyOnWriteArrayList

/**
 * A process-lifetime singleton that hands out no automatic unregistration — the kind of "event bus"
 * or "callback manager" that ViewModels are routinely registered into and forgotten in.
 *
 * Whatever is added here lives until the process dies.
 */
object GlobalListenerRegistry {

    private val registered = CopyOnWriteArrayList<Any>()

    fun register(owner: Any) {
        registered.add(owner)
    }

    fun unregister(owner: Any) {
        registered.remove(owner)
    }

    val size: Int get() = registered.size

    /** e.g. "LeakyRegistryViewModel x4" — makes accumulation visible on screen. */
    fun describe(): String {
        if (registered.isEmpty()) return "empty"
        return registered
            .groupingBy { it.javaClass.simpleName }
            .eachCount()
            .entries
            .sortedBy { it.key }
            .joinToString(", ") { "${it.key} x${it.value}" }
    }
}
