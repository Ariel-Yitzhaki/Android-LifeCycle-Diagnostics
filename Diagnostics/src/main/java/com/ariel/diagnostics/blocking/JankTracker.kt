package com.ariel.diagnostics.blocking

import android.app.Activity
import androidx.metrics.performance.JankStats
import java.util.Locale

/**
 * Counts the frames each Activity draws while it is on screen, and how many of those were late,
 * using AndroidX JankStats. Prints one finding when a screen stops having dropped too many.
 *
 * A JankStats object watches one Window, so this keeps one record per visible Activity. Findings
 * name that Activity rather than asking ForegroundActivityTracker, which by onStop holds no name.
 */
class JankTracker(private val logger: BlockingLogger) {

    // Holding Activities as keys is safe only because every entry is taken back out in
    // stopTracking(), and the framework always calls onStop before onDestroy.
    //
    // No locking: both functions that touch this map are lifecycle callbacks on the main thread,
    // which is why the frame counting below does not come through here.
    private val records = HashMap<Activity, ScreenJankRecord>()

    // Counting starts at onStart rather than onCreate because frames only exist while a screen is
    // visible, and restarts from zero on every visit.
    fun startTracking(activity: Activity) {
        if (records.containsKey(activity)) {
            // Should not happen, since onStart and onStop alternate, but a second JankStats on the
            // same window would double every count.
            return
        }

        // JankStats attaches to the window's decor view, and createAndTrack throws when there is
        // none. Such a screen draws nothing worth counting anyway.
        if (activity.window.peekDecorView() == null) {
            return
        }

        val record = ScreenJankRecord(activity.javaClass.simpleName)

        // From Android 7 onwards this listener does not run on the main thread, which is why the
        // counters it touches are @Volatile and why it does not go near the map above.
        val jankStats = JankStats.createAndTrack(activity.window) { frameData ->
            val janky = frameData.isJank
            record.totalFrames++
            if (janky) {
                record.jankyFrames++
            }
        }

        // Filled in now rather than passed to the constructor, because the listener above had to be
        // able to reach the record.
        record.jankStats = jankStats
        records[activity] = record
    }

    // Prints a finding if the Activity dropped too many frames during this visit.
    fun stopTracking(activity: Activity) {
        val record = records.remove(activity)
        if (record == null) {
            return
        }

        val jankStats = record.jankStats
        if (jankStats != null) {
            // Dropping the reference would not detach JankStats: the window would still hold its
            // listener and carry on doing frame-timing work for a screen nobody is looking at.
            jankStats.isTrackingEnabled = false
        }

        reportIfJanky(record)
    }

    // One line per visit, never per frame.
    private fun reportIfJanky(record: ScreenJankRecord) {
        if (record.totalFrames == 0) {
            return
        }

        val jankPercent = record.jankPercent()
        if (jankPercent <= BlockingConstants.JANK_PERCENT_THRESHOLD) {
            return
        }

        // Locale.US so the decimal separator is always a dot.
        val percentText = String.format(Locale.US, "%.1f", jankPercent)

        logger.report(
            "$percentText% of ${record.activityName}'s frames were dropped while it was on screen " +
                "(${record.jankyFrames} of ${record.totalFrames}, over the " +
                "${BlockingConstants.JANK_PERCENT_THRESHOLD}% limit). A dropped frame is one that " +
                "took more than twice as long as this device's frame budget allows",
        )
    }
}
