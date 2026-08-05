package com.ariel.diagnostics.blocking

/**
 * The frame counts for one screen for as long as that screen was the one in front of the user.
 *
 * One of these is created every time the foreground screen changes and closed when it changes
 * again, so the numbers always describe a single visit to a single screen rather than everything
 * one window drew over a session.
 */
class ScreenJankRecord(

    /** Simple class name of the Activity or Fragment these counts belong to. */
    val screenName: String,
) {

    /**
     * How many frames were drawn while this screen was in front.
     *
     * @Volatile because frames are counted on the thread JankStats delivers them on while the
     * totals are read on the main thread when the screen changes.
     */
    @Volatile
    var totalFrames = 0

    /** How many of those [totalFrames] took long enough to count as jank. */
    @Volatile
    var jankyFrames = 0

    // The caller must check that at least one frame was drawn.
    fun jankPercent(): Double {
        // toDouble() keeps the result a fraction; two Ints divided in Kotlin give an Int.
        return jankyFrames.toDouble() * 100.0 / totalFrames
    }
}
