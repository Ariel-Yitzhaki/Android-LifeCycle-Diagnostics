package com.ariel.diagnostics.leaks

import android.util.Log

/**
 * Prints this feature's findings to Logcat, one human-readable line each.
 *
 * This is the only class in the feature that touches Log, so changing the output format — or
 * sending findings somewhere other than Logcat later — means changing this file only.
 */
class LeakLogger {

    /**
     * Prints one finding as a single line, for example:
     *
     * `ActivityLeakActivity (Activity) was still in memory 3 of 4 times it was destroyed this
     * session — the usual causes are ...`
     *
     * Called by [LeakTally], once per screen class, at the moment that class's numbers first cross
     * both of the thresholds it checks. The hint at the end is the same fixed sentence every time:
     * this feature knows that something is holding the screen but not what, so it lists the three
     * things that are nearly always responsible instead of guessing.
     */
    fun report(record: ScreenLeakRecord) {
        // Findings go out at warn level so they stand out in Logcat and can be filtered on their
        // own. Feature 1 and Feature 2 use the same split: warn for problems, debug for the
        // ordinary running commentary.
        Log.w(
            LeakConstants.LOG_TAG,
            "${record.label()} was still in memory ${record.retainedCount} of " +
                "${record.destroyedCount} times it was destroyed this session — the usual causes " +
                "are a static or companion object field still pointing at it, a listener or " +
                "receiver that was registered and never unregistered, and an inner class, Handler " +
                "or Runnable that outlived the screen",
        )
    }
}
