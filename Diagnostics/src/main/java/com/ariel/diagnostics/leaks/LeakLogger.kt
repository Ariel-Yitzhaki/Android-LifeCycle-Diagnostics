package com.ariel.diagnostics.leaks

import android.util.Log

/**
 * Prints this feature's findings to Logcat, one human-readable line each. The only class in this
 * feature that touches Log.
 */
class LeakLogger {

    // Prints one finding at warn level, for example:
    // ActivityLeakActivity (Activity) was still in memory 3 of 4 times it was destroyed this
    // session — the usual causes are ...
    //
    // The hint is a fixed sentence: this feature knows a screen is being held but not by what, so it
    // lists the three usual causes instead of guessing.
    fun report(record: ScreenLeakRecord) {
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
