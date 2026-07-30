package com.ariel.diagnostics.callbacks

import android.util.Log

/**
 * Prints this feature's findings to Logcat, one human-readable line each.
 *
 * This is the only class in the feature that touches Log, so changing the output format — or
 * sending findings somewhere other than Logcat later — means changing this file only.
 */
class ValidationLogger {

    /**
     * Prints one finding as a single line, for example:
     *
     * `HomeActivity@3f2a1b (Activity) was destroyed without ever reaching resumed`
     *
     * Called by [TransitionTracker], [RecreateWatcher] and [StrictModeWatcher] whenever one of them
     * decides something is wrong. [subject] is what the finding is about and [problem] is what is
     * wrong with it, written so that the two read as one sentence.
     */
    fun report(subject: String, problem: String) {
        // Findings go out at warn level so they stand out in Logcat and can be filtered away from
        // the note() lines below. Feature 1 uses the same split: warn for problems, debug for the
        // ordinary running commentary.
        Log.w(ValidationConstants.LOG_TAG, "$subject $problem")
    }

    /**
     * Prints one ordinary line that is not a finding, such as the confirmation that StrictMode was
     * switched on. Called by [StrictModeWatcher] at install time.
     */
    fun note(message: String) {
        Log.d(ValidationConstants.LOG_TAG, message)
    }
}
