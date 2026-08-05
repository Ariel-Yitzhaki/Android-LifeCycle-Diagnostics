package com.ariel.diagnostics.blocking

/** Tunable values for the main-thread blocking feature. */
object BlockingConstants {

    /** Logcat tag the whole feature prints under. */
    const val LOG_TAG = "MainThreadBlocking"

    /**
     * One main-thread message still running after this many milliseconds is reported as blocking.
     * Well above ordinary work, since a message is meant to finish inside a 16 ms frame, and well
     * below the system's own 5 second "Application Not Responding" limit.
     */
    const val SLOW_MESSAGE_THRESHOLD_MS = 200L

    /**
     * How many frames of the captured main-thread stack are printed with a finding. The top of the
     * stack says what is running right now; a full Android stack is around sixty frames of mostly
     * framework plumbing.
     */
    const val STACK_FRAMES_LOGGED = 8

    /**
     * A screen that dropped more than this percentage of its frames gets a finding when it stops.
     * Five per cent is roughly one dropped frame in twenty.
     */
    const val JANK_PERCENT_THRESHOLD = 5.0
}
