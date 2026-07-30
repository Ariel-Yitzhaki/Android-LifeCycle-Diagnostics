package com.ariel.diagnostics.lifecycle

/**
 * Every tunable number and string this feature uses, in one place. Changing what counts as "slow"
 * should never mean reading the code that measures it.
 */
object DiagnosticsConstants {

    /** The single Logcat tag the whole feature prints under, so `adb logcat -s LifecycleDiagnostics` shows everything. */
    const val LOG_TAG = "LifecycleDiagnostics"

    /**
     * A lifecycle callback that takes longer than this is reported as slow.
     *
     * TODO: replace this with a device-relative threshold. 50 ms is most of a frame on a phone that
     *  renders at 60 Hz and nearly three frames on one that renders at 120 Hz, and a cheap emulator
     *  is slow at everything, so one fixed number over-reports on weak hardware and under-reports on
     *  strong hardware. The plan is to derive the threshold from the device's own frame budget.
     */
    const val SLOW_CALLBACK_THRESHOLD_MS = 50L

    /**
     * How many nanoseconds are in a millisecond.
     *
     * Durations are measured and compared in nanoseconds because that is what System.nanoTime()
     * returns, and only converted to milliseconds at the moment they are printed.
     */
    const val NANOS_PER_MILLI = 1_000_000L
}
