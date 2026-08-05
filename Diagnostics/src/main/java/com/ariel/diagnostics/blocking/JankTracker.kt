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
    // which is why the frame counting below deliberately does not come through here.
    private val records = HashMap<Activity, ScreenJankRecord>()

    // Starts counting frames for one Activity. Counting starts at onStart rather than onCreate
    // because frames only exist while a screen is visible, and restarts from zero on every visit.
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

        // Two things about this listener are easy to get wrong: from Android 7 onwards it does not
        // run on the main thread, which is why the counters it touches are @Volatile and why it does
        // not go near the map above; and the FrameData it is given is reused on the next frame, so
        // anything needed from it has to be copied out before the listener returns.
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

    // Stops counting frames for one Activity and prints a finding if it dropped too many of them.
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

    // Prints a finding for the record if this visit dropped more than the allowed share of frames.
    // One line per visit, never per frame.
    //
    // TODO: ignore visits below some number of frames — a screen that drew ten frames crosses the
    //  threshold on a single late one.
    private fun reportIfJanky(record: ScreenJankRecord) {
        if (record.totalFrames == 0) {
            return
        }

        val jankPercent = record.jankPercent()
        if (jankPercent <= BlockingConstants.JANK_PERCENT_THRESHOLD) {
            return
        }

        // Locale.US so the decimal separator is always a dot and the lines stay greppable.
        val percentText = String.format(Locale.US, "%.1f", jankPercent)

        logger.report(
            "$percentText% of ${record.activityName}'s frames were dropped while it was on screen " +
                "(${record.jankyFrames} of ${record.totalFrames}, over the " +
                "${BlockingConstants.JANK_PERCENT_THRESHOLD}% limit) — a dropped frame is one that " +
                "took more than twice as long as this device's frame budget allows",
        )
    }
}
