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
     * How many frames of a StrictMode violation's own stack are printed with a finding. A thread
     * violation carries no message, so this stack is the only thing that says which call touched
     * the disk or the network.
     */
    const val VIOLATION_FRAMES_LOGGED = 8

    /**
     * A screen that dropped more than this percentage of its frames gets a finding when it stops.
     * Five per cent is roughly one dropped frame in twenty.
     */
    const val JANK_PERCENT_THRESHOLD = 5.0

    /**
     * How many frames a screen must have drawn before its percentage is worth believing. Frames are
     * counted per screen rather than per Activity, and a screen the user passed straight through
     * may have drawn only a handful, where one late frame is already twenty five per cent.
     */
    const val MIN_FRAMES_COUNTED = 20

    /**
     * How long a screen's main thread work is added up for after it comes to the front. Long enough
     * to cover the loading a screen starts in a callback and finishes afterwards, short enough that
     * a screen the user then sits and reads for a minute does not dilute the answer to nothing.
     */
    const val SETTLE_WINDOW_MS = 5_000L

    /**
     * A screen that kept the main thread busy for more than this percentage of the window above is
     * reported. Half of it is well past what putting a screen up should need, and a screen over
     * this line is still working long after every one of its lifecycle callbacks has returned.
     */
    const val BUSY_PERCENT_THRESHOLD = 50.0

    /**
     * How long a screen must have been in front before its busy percentage is worth believing. A
     * screen the user passed straight through can spend every one of its few milliseconds inside a
     * single message without that meaning anything.
     */
    const val MIN_SETTLE_MS = 500L
}
