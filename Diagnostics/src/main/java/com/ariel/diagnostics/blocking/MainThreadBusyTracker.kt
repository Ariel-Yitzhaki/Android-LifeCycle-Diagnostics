package com.ariel.diagnostics.blocking

import android.os.SystemClock
import java.util.Locale

/**
 * Adds up how much of the main thread each screen uses in its first seconds, and reports a screen
 * that spends most of them working.
 *
 * This is the one detector that looks at a screen rather than at a moment. Lifecycle timings only
 * cover the inside of a callback, and a screen that loads its content in a coroutine or a listener
 * does that work in later messages of its own, so every callback returns in a millisecond and the
 * timings say the screen is fine while the user waits three seconds for it. The slow-message
 * detector does not fill the gap either: it only fires on one message crossing the threshold, and
 * two hundred messages of eighty milliseconds never do.
 *
 * Counting stops after [BlockingConstants.SETTLE_WINDOW_MS], because a screen the user is working
 * in is meant to keep the main thread busy. The question here is only how long it takes to settle.
 */
class MainThreadBusyTracker(
    private val logger: BlockingLogger,
    private val foregroundScreen: ForegroundScreenTracker,
) {

    // The screen being counted, or null when there is nothing in front or the window has closed.
    //
    // No locking and nothing @Volatile: both functions below run on the main thread, one from the
    // Looper's Printer and one from a lifecycle callback.
    private var record: ScreenBusyRecord? = null

    // Called after every main-thread message, with how long that message ran.
    fun onMessageFinished(messageMillis: Long) {
        val open = record
        if (open == null) {
            return
        }

        val elapsedMillis = SystemClock.uptimeMillis() - open.startUptimeMillis
        if (elapsedMillis > BlockingConstants.SETTLE_WINDOW_MS) {
            // The window has closed. Reported here rather than on a timer, because the answer only
            // changes when a message ends, so there is nothing to wait for.
            closeAndReport()
            return
        }

        // A message can only count for the part of it that ran after the screen came to the front.
        // The transaction that puts a screen up is still running when that screen's onResume
        // arrives, so its first message is nearly always one that started earlier.
        var countedMillis = messageMillis
        if (countedMillis > elapsedMillis) {
            countedMillis = elapsedMillis
        }

        open.busyMillis += countedMillis
        open.messageCount++
    }

    // Called whenever an Activity or a Fragment resumes or pauses, the same moments JankTracker is
    // told about.
    fun onForegroundScreenChanged() {
        val screenName = foregroundScreen.currentScreenName()
        if (screenName == null) {
            // Nothing in front, which is the app going to the background. Closed rather than left
            // open, so whatever comes back does not inherit a window that started minutes ago.
            closeAndReport()
            return
        }

        val open = record
        if (open != null && open.screenName == screenName) {
            // The screen in front has not actually changed, so its window carries on running.
            return
        }

        closeAndReport()
        record = ScreenBusyRecord(screenName, SystemClock.uptimeMillis())
    }

    private fun closeAndReport() {
        val finished = record
        // Cleared first, so a report can never be printed twice for one window.
        record = null

        if (finished == null) {
            return
        }
        reportIfSlowToSettle(finished)
    }

    private fun reportIfSlowToSettle(record: ScreenBusyRecord) {
        // However much of the window the screen actually got. A screen the user left after a second
        // is judged on that second rather than on the full five.
        var watchedMillis = SystemClock.uptimeMillis() - record.startUptimeMillis
        if (watchedMillis > BlockingConstants.SETTLE_WINDOW_MS) {
            watchedMillis = BlockingConstants.SETTLE_WINDOW_MS
        }

        if (watchedMillis < BlockingConstants.MIN_SETTLE_MS) {
            return
        }

        // Cannot pass 100: main-thread messages do not overlap, and the one message that can start
        // before the window is trimmed to fit it in onMessageFinished.
        val busyPercent = record.busyMillis * 100.0 / watchedMillis
        if (busyPercent <= BlockingConstants.BUSY_PERCENT_THRESHOLD) {
            return
        }

        // Locale.US so the decimal separator is always a dot.
        val percentText = String.format(Locale.US, "%.1f", busyPercent)

        logger.report(
            "${record.screenName} kept the main thread busy for $percentText% of the first " +
                "$watchedMillis ms it was in front (${record.busyMillis} ms across " +
                "${record.messageCount} messages, over the " +
                "${BlockingConstants.BUSY_PERCENT_THRESHOLD}% limit). That covers its own lifecycle " +
                "callbacks and everything it started in them and finished afterwards, which no " +
                "single callback timing can show",
        )
    }
}
