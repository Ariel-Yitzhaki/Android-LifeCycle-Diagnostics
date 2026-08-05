package com.ariel.diagnostics.blocking

import android.app.Activity
import android.app.Application
import android.os.Bundle

/**
 * Passes the Activity lifecycle callbacks this feature needs on to the pieces that need them: start
 * and stop to JankTracker, resume and pause to ForegroundActivityTracker, and the moment before the
 * first Activity is created to StrictModeThreadWatcher.
 */
class BlockingActivityCallbacks(
    private val strictModeThreadWatcher: StrictModeThreadWatcher,
    private val jankTracker: JankTracker,
    private val foregroundActivity: ForegroundActivityTracker,
) : Application.ActivityLifecycleCallbacks {

    // Runs immediately before an Activity's own onCreate — after the app's Application.onCreate has
    // finished but before any screen exists, which is the only moment the StrictMode thread policy
    // can be installed. See StrictModeThreadWatcher.installIfNeeded.
    override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
        strictModeThreadWatcher.installIfNeeded(activity)
    }

    // Runs after an Activity's own onStart, the point from which it starts drawing frames.
    override fun onActivityStarted(activity: Activity) {
        jankTracker.startTracking(activity)
    }

    // Runs after an Activity's own onResume, the newest answer to which screen the user is on.
    override fun onActivityResumed(activity: Activity) {
        foregroundActivity.onActivityResumed(activity.javaClass.simpleName)
    }

    override fun onActivityPaused(activity: Activity) {
        foregroundActivity.onActivityPaused()
    }

    // Runs after an Activity's own onStop, the end of a visit and so the moment frame counts are
    // final.
    override fun onActivityStopped(activity: Activity) {
        jankTracker.stopTracking(activity)
    }

    // Nothing this feature measures starts or ends at these moments, but they have no default
    // implementation.

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}
}
