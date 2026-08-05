package com.ariel.diagnostics.lifecycle

/** One measurement taken at one lifecycle callback on one screen. */
data class LifecycleTiming(

    /** Simple class name of the Activity or Fragment, for example "HomeActivity". */
    val screenName: String,

    /** The callback this was measured at, for example "onCreate". */
    val callbackName: String,

    /** The measured duration in nanoseconds. See [kind] for what it is a duration of. */
    val durationNanos: Long,

    /** True when this is the first measurement recorded for this screen class in this process. */
    val firstSeen: Boolean,

    /** True when the screen was going through a configuration change rather than normal navigation. */
    val configurationChange: Boolean,

    /**
     * What [durationNanos] is a duration of, which decides how the line is worded and whether the
     * measurement can be called slow at all.
     */
    val kind: MeasurementKind,
)
