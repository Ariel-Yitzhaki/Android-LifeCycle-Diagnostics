package com.ariel.diagnostics.callbacks

import android.util.Log

/** Prints this feature's findings to Logcat, one line each. */
class ValidationLogger {

    // Prints one finding at warn level, for example:
    // HomeActivity@3f2a1b (Activity) was destroyed without ever reaching resumed
    fun report(subject: String, problem: String) {
        Log.w(ValidationConstants.LOG_TAG, "$subject $problem")
    }

    // Prints one ordinary line at debug level, such as the confirmation that StrictMode is on.
    fun note(message: String) {
        Log.d(ValidationConstants.LOG_TAG, message)
    }
}
