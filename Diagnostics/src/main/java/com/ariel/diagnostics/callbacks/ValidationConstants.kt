package com.ariel.diagnostics.callbacks

/**
 * Every tunable number and string this feature uses, in one place. Changing what counts as thrash
 * should never mean reading the code that detects it.
 */
object ValidationConstants {

    /**
     * The single Logcat tag the whole feature prints under, so `adb logcat -s CallbackValidation`
     * shows every finding and nothing else.
     *
     * Feature 1 uses one tag for itself in the same way. This feature gets its own tag rather than
     * borrowing Feature 1's so that timing lines and validation findings can be filtered apart.
     */
    const val LOG_TAG = "CallbackValidation"

    /**
     * How many destroy/recreate cycles of one Activity class inside [RECREATE_WINDOW_MS] still
     * count as normal. The cycle after this one is reported as thrash.
     *
     * Three is deliberately low: a user tapping into a screen and pressing Back a few times in ten
     * seconds is unusual but harmless, while a screen that restarts itself in a loop crosses this
     * within a second or two.
     */
    const val RECREATE_LIMIT = 3

    /** The length of the window the cycles above are counted in, in milliseconds. */
    const val RECREATE_WINDOW_MS = 10_000L
}
