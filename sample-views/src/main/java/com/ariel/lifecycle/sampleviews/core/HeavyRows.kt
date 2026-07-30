package com.ariel.lifecycle.sampleviews.core

import java.util.concurrent.ConcurrentHashMap

/** Row content for the jank screens: expensive to produce, cheap to display. */
object HeavyRows {

    const val ROW_COUNT = 400

    /** Comfortably over one frame at 60 Hz, so a single row is enough to drop a frame. */
    const val COST_MS = 12L

    private val cache = ConcurrentHashMap<Int, String>()

    /** Costs [COST_MS] on every call — no memoisation, by design. */
    fun compute(index: Int): String {
        val iterations = BusyWork.spin(COST_MS, seed = index.toLong() * 31 + 17)
        return "Row $index — ${iterations / 1000}K iterations"
    }

    /** Non-blocking peek used by the clean screens to bind instantly once a row is known. */
    fun cached(index: Int): String? = cache[index]

    /** Computes once, then serves from memory. Still costs [COST_MS] the first time per row. */
    fun computeAndCache(index: Int): String = cache[index] ?: compute(index).also { cache[index] = it }
}
