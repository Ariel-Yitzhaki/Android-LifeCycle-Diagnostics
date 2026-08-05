package com.ariel.diagnostics.lifecycle

/** Tunable values for the lifecycle timing feature. */
object DiagnosticsConstants {

    /** Logcat tag the whole feature prints under. */
    const val LOG_TAG = "LifecycleDiagnostics"

    /** A lifecycle callback taking longer than this is reported as slow. */
    const val SLOW_CALLBACK_THRESHOLD_MS = 50L

    /** Durations are measured in nanos and converted for printing. */
    const val NANOS_PER_MILLI = 1_000_000L
}
