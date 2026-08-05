package com.ariel.diagnostics.blocking

import android.util.Printer

/**
 * Recognises the two lines the main Looper prints around every message it runs, and turns them into
 * "a message started" and "a message finished" for SlowMessageWatchdog.
 *
 * This hook is the only way an app can time its own main-thread work at runtime; everything else
 * that can answer the question is a tool attached from outside the process. See MainThreadBlocking
 * for how it is attached and for the catch that there is only room for one printer.
 */
class MainThreadPrinter(private val watchdog: SlowMessageWatchdog) : Printer {

    // The full line also names the Handler and the target, but the marker at the front is all that
    // is needed. Both markers are fixed inside Looper.loopOnce().
    private val dispatchMarker = ">>>>> Dispatching to"
    private val finishMarker = "<<<<< Finished to"

    // Called by the Looper on the main thread, twice for every message it runs — thousands of times
    // a minute in a busy app, which is why this has to stay as short as it looks. startsWith()
    // allocates nothing and stops at the first difference.
    override fun println(x: String) {
        if (x.startsWith(dispatchMarker)) {
            watchdog.onMessageStarted()
        } else if (x.startsWith(finishMarker)) {
            watchdog.onMessageFinished()
        }
    }
}
