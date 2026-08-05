package com.ariel.diagnostics.lifecycle

/**
 * What a [LifecycleTiming]'s duration actually measures, which is not the same for every callback.
 *
 * Activities have a Pre/Post pair around all six callbacks, so every Activity measurement is
 * [EXACT]. Fragments have a real pair only around onCreate, so the rest are timed from the previous
 * callback, and one of those gaps does not contain work at all.
 */
enum class MeasurementKind {

    /** Bracketed by a real before/after pair, so the duration is the callback's own. */
    EXACT,

    /**
     * The gap since the previous callback, because the framework offers no hook before this one.
     * Close to the callback's own cost when the two run back to back, but it also picks up anything
     * the framework or another component did in between.
     */
    BETWEEN_CALLBACKS,

    /**
     * The gap from onResume to onPause, which is the time the user spent looking at the screen and
     * not work of any kind.
     *
     * Reported for information only and never marked slow: on a screen the user reads for a minute
     * this is sixty thousand milliseconds, and the fragment's own onPause cost is lost inside it
     * either way.
     */
    TIME_ON_SCREEN,
}
