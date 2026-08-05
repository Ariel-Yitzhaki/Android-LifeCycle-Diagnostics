package com.ariel.diagnostics.lifecycle

/** Tunable values for the lifecycle timing feature. */
object DiagnosticsConstants {

    /** Logcat tag the whole feature prints under. */
    const val LOG_TAG = "LifecycleDiagnostics"

    /**
     * A lifecycle callback taking longer than this is reported as slow.
     *
     * TODO: derive this from the device's own frame budget instead of a fixed number, which
     *  over-reports on weak hardware and under-reports on strong hardware.
     */
    const val SLOW_CALLBACK_THRESHOLD_MS = 50L

    /** Nanoseconds per millisecond. Durations are measured in nanos and converted only for printing. */
    const val NANOS_PER_MILLI = 1_000_000L
}
