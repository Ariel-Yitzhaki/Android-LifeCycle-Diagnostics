package com.ariel.diagnostics.callbacks

/** Tunable values for the callback validation feature. */
object ValidationConstants {

    /** Logcat tag the whole feature prints under. */
    const val LOG_TAG = "CallbackValidation"

    /**
     * How many destroy/recreate cycles of one Activity class inside [RECREATE_WINDOW_MS] still count
     * as normal. The cycle after this one is reported as thrash.
     */
    const val RECREATE_LIMIT = 3

    /** The length of the window the cycles above are counted in, in milliseconds. */
    const val RECREATE_WINDOW_MS = 10_000L
}
