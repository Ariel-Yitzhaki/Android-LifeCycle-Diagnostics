package com.ariel.diagnostics.blocking

import androidx.metrics.performance.JankStats

/**
 * The frame counts for one Activity for as long as it is on screen, plus the JankStats object
 * feeding them, which has to be switched off again when the screen stops.
 *
 * One of these is created every time an Activity becomes visible and thrown away when it stops, so
 * the numbers always describe a single visit to a single screen.
 */
class ScreenJankRecord(

    /** Simple class name of the Activity these counts belong to, for example "JankListActivity". */
    val activityName: String,
) {

    /**
     * The JankStats object delivering frames into these counts. Null only between this record being
     * created and JankTracker.startTracking filling it in.
     */
    var jankStats: JankStats? = null

    /**
     * How many frames this screen has drawn during this visit.
     *
     * @Volatile because frames are counted on JankStats' background thread while the totals are
     * read on the main thread when the screen stops.
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
