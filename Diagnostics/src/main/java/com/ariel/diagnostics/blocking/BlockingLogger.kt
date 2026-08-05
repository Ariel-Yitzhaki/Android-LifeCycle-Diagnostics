package com.ariel.diagnostics.blocking

import android.util.Log

/** Prints this feature's findings to Logcat, one line each. */
class BlockingLogger {

    // Prints one finding at warn level, for example:
    // the main thread has been busy with one message for 213 ms ...
    fun report(finding: String) {
        Log.w(BlockingConstants.LOG_TAG, finding)
    }

    // Prints one ordinary line at debug level, such as the startup lines printed at install time.
    fun note(message: String) {
        Log.d(BlockingConstants.LOG_TAG, message)
    }
}
