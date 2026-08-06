package com.ariel.lifecycle.sampleviews.core

import android.view.View

/**
 * A process-lifetime cache of Views — the kind of "keep the header around so it does not have to be
 * inflated again" optimisation that quietly keeps a whole Activity alive, because every View holds
 * the Context it was inflated with.
 *
 * Nothing here ever points at a Fragment, which is the difference that matters: the fragment whose
 * view is cached here can still be collected, so the library sees a Fragment view retained while its
 * Fragment is gone rather than the back-stack case.
 */
object RetainedViews {

    private val cached = mutableListOf<View>()

    fun keep(view: View) {
        cached += view
    }

    val size: Int get() = cached.size

    /** e.g. "ScrollView x3" — makes the accumulation visible on screen. */
    fun describe(): String {
        if (cached.isEmpty()) return "empty"
        return cached
            .groupingBy { it.javaClass.simpleName }
            .eachCount()
            .entries
            .sortedBy { it.key }
            .joinToString(", ") { "${it.key} x${it.value}" }
    }
}
