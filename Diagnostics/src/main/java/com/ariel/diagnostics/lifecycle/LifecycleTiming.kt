package com.ariel.diagnostics.lifecycle

/** One measurement: how long one lifecycle callback took on one screen. */
data class LifecycleTiming(

    /** Simple class name of the Activity or Fragment, for example "HomeActivity". */
    val screenName: String,

    /** Which callback this measures, for example "onCreate". */
    val callbackName: String,

    /** How long the callback took, in nanoseconds. */
    val durationNanos: Long,

    /** True when this is the first measurement recorded for this screen class in this process. */
    val firstSeen: Boolean,

    /** True when the screen was going through a configuration change rather than normal navigation. */
    val configurationChange: Boolean,

    /**
     * True when the duration is the gap between two consecutive callbacks rather than a real
     * before/after pair around a single callback.
     */
    val approximate: Boolean,
)
