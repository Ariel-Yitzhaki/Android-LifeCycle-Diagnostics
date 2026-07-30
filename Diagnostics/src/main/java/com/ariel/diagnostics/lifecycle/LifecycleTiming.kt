package com.ariel.diagnostics.lifecycle

/**
 * One measurement: how long one lifecycle callback took on one screen.
 *
 * It is built the instant the callback ends, handed straight to [TimingLogger], and then dropped.
 * Nothing keeps a list of these — this feature prints and forgets.
 */
data class LifecycleTiming(

    /** Simple class name of the Activity or Fragment, for example "HomeActivity". */
    val screenName: String,

    /** Which callback this measures, for example "onCreate". */
    val callbackName: String,

    /** How long the callback took, in nanoseconds. Converted to milliseconds only for printing. */
    val durationNanos: Long,

    /** True when this is the first measurement recorded for this screen class in this process. */
    val firstSeen: Boolean,

    /**
     * True when the screen was going through a configuration change (a rotation, a theme switch, a
     * font-size change) rather than a normal navigation. Only ever true on the way out — see
     * [ActivityTimingCallbacks] for why.
     */
    val configurationChange: Boolean,

    /**
     * True when the duration is the gap between two consecutive callbacks rather than a real
     * before/after pair around a single callback. Fragments have no "before" half for most of their
     * callbacks, so most fragment measurements are approximate and include work that happened
     * between the two callbacks as well as the callback itself.
     */
    val approximate: Boolean,
)
