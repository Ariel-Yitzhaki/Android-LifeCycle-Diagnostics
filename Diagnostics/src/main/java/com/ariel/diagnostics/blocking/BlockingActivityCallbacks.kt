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

    // Runs after the app's Application.onCreate has finished but before any screen exists, the only
    // moment the StrictMode thread policy can be installed. See
    // StrictModeThreadWatcher.installIfNeeded.
    override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
        strictModeThreadWatcher.installIfNeeded(activity)
    }

    // onStart is the point from which an Activity starts drawing frames.
    override fun onActivityStarted(activity: Activity) {
        jankTracker.startTracking(activity)
    }

    // The newest answer to which screen the user is on.
    override fun onActivityResumed(activity: Activity) {
        foregroundActivity.onActivityResumed(activity.javaClass.simpleName)
    }

    override fun onActivityPaused(activity: Activity) {
        foregroundActivity.onActivityPaused()
    }

    // onStop ends a visit, so this is the moment the frame counts are final.
    override fun onActivityStopped(activity: Activity) {
        jankTracker.stopTracking(activity)
    }

    // Not needed by this feature, but the three callbacks below have no default implementation.

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}
}
