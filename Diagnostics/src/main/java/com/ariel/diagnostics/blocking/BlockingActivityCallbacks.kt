package com.ariel.diagnostics.blocking

import android.app.Activity
import android.app.Application
import android.os.Bundle

/**
 * Routes the Activity lifecycle callbacks this feature needs: start and stop to JankTracker, resume
 * and pause to ForegroundActivityTracker, and the moment before the first Activity is created to
 * StrictModeThreadWatcher.
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

    override fun onActivityStarted(activity: Activity) {
        jankTracker.startTracking(activity)
    }

    override fun onActivityResumed(activity: Activity) {
        foregroundActivity.onActivityResumed(activity.javaClass.simpleName)
    }

    override fun onActivityPaused(activity: Activity) {
        foregroundActivity.onActivityPaused()
    }

    override fun onActivityStopped(activity: Activity) {
        jankTracker.stopTracking(activity)
    }

    // Not needed by this feature, but the three callbacks below have no default implementation.

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}
}
