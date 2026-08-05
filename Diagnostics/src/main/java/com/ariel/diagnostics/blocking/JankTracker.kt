package com.ariel.diagnostics.blocking

import android.view.Window
import androidx.metrics.performance.JankStats
import java.util.Locale

/**
 * Counts the frames drawn while each screen is in front of the user, and how many of those were
 * late, using AndroidX JankStats. Prints one finding when a screen leaves having dropped too many.
 *
 * A JankStats object watches one Window, but a window is not a screen. In an app built as one
 * Activity hosting fragments there is one window for the whole session, so counting per window
 * would give a single figure at the end that averages a list dropping half its frames against
 * minutes of a screen sitting still. The counts are therefore cut whenever the foreground screen
 * changes, and each stretch is reported on its own.
 *
 * A DialogFragment draws into a window of its own on top of the Activity's, so it gets a counter of
 * its own as well. Plain Dialogs an app builds and shows itself cannot be seen from here, since the
 * framework offers no callback for them.
 */
class JankTracker(
    private val logger: BlockingLogger,
    private val foregroundScreen: ForegroundScreenTracker,
) {

    // One entry per window being counted, keyed by whatever owns that window: every started
    // Activity, and every started DialogFragment on top of one.
    //
    // Holding those objects as keys is safe only because every entry is taken back out in
    // stopTracking(), and the framework always calls onStop before onDestroy.
    //
    // No locking: every function that touches this map is a lifecycle callback on the main thread,
    // which is why the frame counting below does not come through here.
    private val counters = HashMap<Any, WindowJankCounter>()

    // Counting starts at onStart rather than onCreate because frames only exist while a window is
    // on screen, and restarts from zero on every visit. screenName is the name the first stretch is
    // counted under, before anything has resumed to give a better one.
    fun startTracking(owner: Any, window: Window, screenName: String) {
        if (counters.containsKey(owner)) {
            // Should not happen, since onStart and onStop alternate, but a second JankStats on the
            // same window would double every count.
            return
        }

        // JankStats attaches to the window's decor view, and createAndTrack throws when there is
        // none. Such a window draws nothing worth counting anyway.
        if (window.peekDecorView() == null) {
            return
        }

        val counter = WindowJankCounter(screenName)

        // From Android 7 onwards this listener does not run on the main thread, which is why the
        // counters it touches are @Volatile and why it does not go near the map above.
        val jankStats = JankStats.createAndTrack(window) { frameData ->
            // Read once into a local, so a frame cannot be counted into one screen's total and
            // another's janky count when the main thread swaps the record in between.
            val record = counter.record
            record.totalFrames++
            if (frameData.isJank) {
                record.jankyFrames++
            }
        }

        // Filled in now rather than passed to the constructor, because the listener above had to be
        // able to reach the counter.
        counter.jankStats = jankStats
        counters[owner] = counter
    }

    // Closes the stretch every window is counting and starts a fresh one under the screen that is
    // in front now. Called whenever an Activity or a Fragment resumes or pauses, which is the only
    // thing that changes the answer while a window stays on screen.
    fun onForegroundScreenChanged() {
        val screenName = foregroundScreen.currentScreenName()
        if (screenName == null) {
            // Nothing is in the foreground, which is the app going to the background. There is no
            // better name to open a stretch under, so the one already open stays and onStop closes
            // it.
            return
        }

        for (counter in counters.values) {
            val finished = counter.record
            if (finished.screenName == screenName) {
                // Every window is named after the same foreground screen, so most calls find
                // nothing to do. Cutting here anyway would split one screen's frames into stretches
                // too short to report.
                continue
            }

            // Swapped before the finished stretch is reported, so the frames still arriving are
            // already being counted against the new screen rather than the one being printed.
            counter.record = ScreenJankRecord(screenName)
            reportIfJanky(finished)
        }
    }

    // Closes the last stretch this window was counting and detaches JankStats. Does nothing for an
    // owner that was never counted, which is every fragment without a window of its own.
    fun stopTracking(owner: Any) {
        val counter = counters.remove(owner)
        if (counter == null) {
            return
        }

        val jankStats = counter.jankStats
        if (jankStats != null) {
            // Dropping the reference would not detach JankStats: the window would still hold its
            // listener and carry on doing frame-timing work for a screen nobody is looking at.
            jankStats.isTrackingEnabled = false
        }

        reportIfJanky(counter.record)
    }

    private fun reportIfJanky(record: ScreenJankRecord) {
        // A stretch ends every time the user moves between fragments, so very short ones are
        // ordinary rather than a fault. One late frame out of four is twenty five per cent and
        // describes nothing. This is also what drops the empty stretches a window collects while a
        // dialog covers it, and the one it opens with before anything has resumed.
        if (record.totalFrames < BlockingConstants.MIN_FRAMES_COUNTED) {
            return
        }

        val jankPercent = record.jankPercent()
        if (jankPercent <= BlockingConstants.JANK_PERCENT_THRESHOLD) {
            return
        }

        // Locale.US so the decimal separator is always a dot.
        val percentText = String.format(Locale.US, "%.1f", jankPercent)

        logger.report(
            "$percentText% of ${record.screenName}'s frames were dropped while it was in front " +
                "(${record.jankyFrames} of ${record.totalFrames}, over the " +
                "${BlockingConstants.JANK_PERCENT_THRESHOLD}% limit). A dropped frame is one that " +
                "took more than twice as long as this device's frame budget allows",
        )
    }
}
