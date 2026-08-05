package com.ariel.diagnostics.blocking

import android.util.Log

/**
 * Prints this feature's findings to Logcat, one human-readable line each. The only class in this
 * feature that touches Log.
 */
class BlockingLogger {

    // Prints one finding at warn level, for example:
    // the main thread was busy with one message for 213 ms ...
    fun report(finding: String) {
        Log.w(BlockingConstants.LOG_TAG, finding)
    }

    // Prints one ordinary line at debug level, such as the startup lines printed at install time.
    fun note(message: String) {
        Log.d(BlockingConstants.LOG_TAG, message)
    }
}
