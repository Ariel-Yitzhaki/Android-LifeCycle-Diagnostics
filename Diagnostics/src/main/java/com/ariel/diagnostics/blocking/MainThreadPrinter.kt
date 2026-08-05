package com.ariel.diagnostics.blocking

import android.util.Printer

/**
 * Recognises the two lines the main Looper prints around every message it runs, and turns them into
 * "a message started" and "a message finished" for SlowMessageWatchdog.
 *
 * See MainThreadBlocking for how it is attached, and for the catch that a Looper has room for only
 * one printer.
 */
class MainThreadPrinter(private val watchdog: SlowMessageWatchdog) : Printer {

    // The full line also names the Handler and the target, but the marker at the front is all that
    // is needed. Both markers are fixed inside Looper.loopOnce().
    private val dispatchMarker = ">>>>> Dispatching to"
    private val finishMarker = "<<<<< Finished to"

    // Called by the Looper on the main thread, twice for every message it runs, which is thousands
    // of times a minute in a busy app. startsWith() allocates nothing and stops at the first
    // difference.
    override fun println(x: String) {
        if (x.startsWith(dispatchMarker)) {
            watchdog.onMessageStarted()
        } else if (x.startsWith(finishMarker)) {
            watchdog.onMessageFinished()
        }
    }
}
