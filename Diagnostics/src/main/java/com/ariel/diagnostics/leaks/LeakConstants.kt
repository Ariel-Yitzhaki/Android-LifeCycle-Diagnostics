package com.ariel.diagnostics.leaks

/** Tunable values for the leak detection feature. */
object LeakConstants {

    /** Logcat tag the whole feature prints under. */
    const val LOG_TAG = "LeakDetection"

    /**
     * How long to wait after a component is destroyed before checking whether it is still in memory.
     * Checking sooner would report almost everything, since the framework itself holds a destroyed
     * screen briefly while it finishes tearing it down.
     */
    const val WATCH_DELAY_MS = 5_000L

    /**
     * How long to pause after asking for a garbage collection. The runtime empties weak references
     * on a thread of its own shortly after the collection, so without this pause a component that
     * has just been collected can still look retained.
     */
    const val GC_SETTLE_MS = 100L

    /** How many destructions of one screen class must be seen before a finding can be printed. */
    const val MIN_DESTROY_COUNT = 3
}
