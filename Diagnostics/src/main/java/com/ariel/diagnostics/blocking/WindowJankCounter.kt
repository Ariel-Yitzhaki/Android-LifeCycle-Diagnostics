package com.ariel.diagnostics.blocking

import androidx.metrics.performance.JankStats

/**
 * One window being counted, and the stretch of that window's frames being counted right now.
 *
 * A window outlives the screens drawn into it: a single Activity window carries every fragment the
 * user visits. So the JankStats object here is created once, when the window appears, while
 * [record] is replaced every time the screen in front changes. That is what keeps a finding about
 * one fragment from being diluted by every other fragment's frames.
 */
class WindowJankCounter(screenName: String) {

    /**
     * The JankStats object delivering frames into [record]. Null only between this counter being
     * created and JankTracker.startTracking filling it in.
     */
    var jankStats: JankStats? = null

    /**
     * The screen whose frames are being counted. Replaced whole, never edited, when the foreground
     * screen changes.
     *
     * @Volatile because it is swapped on the main thread and read on the thread JankStats delivers
     * frames on. A frame arriving during the swap lands in whichever record that thread had already
     * read, so the boundary between two screens is accurate to about one frame.
     */
    @Volatile
    var record = ScreenJankRecord(screenName)
}
