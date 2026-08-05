package com.ariel.diagnostics.leaks

import android.util.Log

/** Prints this feature's findings to Logcat, one line each. */
class LeakLogger {

    // The hint is a fixed sentence: this feature knows a screen is being held but not by what, so
    // it lists the usual causes instead of guessing.
    fun report(record: ScreenLeakRecord) {
        Log.w(
            LeakConstants.LOG_TAG,
            "${record.label()} was still in memory ${record.retainedCount} of " +
                "${record.destroyedCount} times it was destroyed this session. The usual causes " +
                "are a static or companion object field still pointing at it, a listener or " +
                "receiver that was registered and never unregistered, and an inner class, Handler " +
                "or Runnable that outlived the screen",
        )
    }
}
