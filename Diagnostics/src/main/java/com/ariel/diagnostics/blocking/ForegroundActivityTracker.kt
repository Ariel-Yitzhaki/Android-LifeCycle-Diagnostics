package com.ariel.diagnostics.blocking

/**
 * Keeps the name of the Activity in the foreground right now, so a finding from either detector can
 * say which screen the user was looking at.
 *
 * TODO: merge with the equivalent field in Feature 2's StrictModeWatcher into one shared tracker.
 */
class ForegroundActivityTracker {

    // A name and never an Activity object: this field lives as long as the process, so holding an
    // Activity here would leak the screen.
    //
    // @Volatile because it is written on the main thread and read from SlowMessageWatchdog's
    // background thread, which would otherwise keep reporting a screen the user left minutes ago.
    @Volatile
    private var foregroundActivityName: String? = null

    // Remembers which Activity is in the foreground. Called on the main thread from onActivityResumed.
    fun onActivityResumed(activityName: String) {
        foregroundActivityName = activityName
    }

    // Forgets the foreground Activity. Called from onActivityPaused, which Android always runs
    // before resuming the incoming Activity, so this cannot wipe out a newer name.
    fun onActivityPaused() {
        foregroundActivityName = null
    }

    // Builds the text a finding uses to say where it happened, for example "with HomeActivity in the
    // foreground".
    fun describe(): String {
        // Read into a local so the value cannot change between the check and the use below.
        val name = foregroundActivityName
        if (name == null) {
            return "with no Activity in the foreground"
        }
        return "with $name in the foreground"
    }
}
