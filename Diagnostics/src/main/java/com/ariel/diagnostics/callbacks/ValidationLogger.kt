package com.ariel.diagnostics.callbacks

import android.util.Log

/**
 * Prints this feature's findings to Logcat, one human-readable line each. The only class in this
 * feature that touches Log.
 */
class ValidationLogger {

    // Prints one finding at warn level, for example:
    // HomeActivity@3f2a1b (Activity) was destroyed without ever reaching resumed
    //
    // The subject and the problem are written so that the two read as one sentence.
    fun report(subject: String, problem: String) {
        Log.w(ValidationConstants.LOG_TAG, "$subject $problem")
    }

    // Prints one ordinary line at debug level, such as the confirmation that StrictMode is on.
    fun note(message: String) {
        Log.d(ValidationConstants.LOG_TAG, message)
    }
}
