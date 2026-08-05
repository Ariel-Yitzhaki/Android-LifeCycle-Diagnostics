package com.ariel.diagnostics.blocking

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.fragment.app.FragmentActivity

/**
 * Routes the Activity lifecycle callbacks this feature needs: start and stop to JankTracker, resume
 * and pause to ForegroundScreenTracker and the two trackers that count per screen, and the moment
 * before the first Activity is created to StrictModeThreadWatcher. Also attaches
 * BlockingFragmentCallbacks to every Activity able to host fragments.
 */
class BlockingActivityCallbacks(
    private val strictModeThreadWatcher: StrictModeThreadWatcher,
    private val jankTracker: JankTracker,
    private val foregroundScreen: ForegroundScreenTracker,
    private val busyTracker: MainThreadBusyTracker,
    private val fragmentCallbacks: BlockingFragmentCallbacks,
) : Application.ActivityLifecycleCallbacks {

    // Runs after the app's Application.onCreate has finished but before any screen exists, the only
    // moment the StrictMode thread policy can be installed. See
    // StrictModeThreadWatcher.installIfNeeded.
    //
    // Also where the fragment callbacks are registered, before the Activity's own onCreate, because
    // super.onCreate() is where a FragmentActivity re-creates its fragments. True asks for them
    // recursively, so a fragment nested inside another is reported as well.
    override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
        strictModeThreadWatcher.installIfNeeded(activity)

        if (activity is FragmentActivity) {
            activity.supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentCallbacks, true)
        }
    }

    // The Activity names the first stretch of frames. Its fragments have not resumed yet, and one
    // of them takes the name over as soon as it does.
    override fun onActivityStarted(activity: Activity) {
        jankTracker.startTracking(activity, activity.window, activity.javaClass.simpleName)
    }

    // The tracker is always told first, since both counters below read the new answer from it.
    override fun onActivityResumed(activity: Activity) {
        foregroundScreen.onActivityResumed(activity.javaClass.simpleName)
        jankTracker.onForegroundScreenChanged()
        busyTracker.onForegroundScreenChanged()
    }

    override fun onActivityPaused(activity: Activity) {
        foregroundScreen.onActivityPaused()
        jankTracker.onForegroundScreenChanged()
        busyTracker.onForegroundScreenChanged()
    }

    override fun onActivityStopped(activity: Activity) {
        jankTracker.stopTracking(activity)
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (activity is FragmentActivity) {
            activity.supportFragmentManager.unregisterFragmentLifecycleCallbacks(fragmentCallbacks)
        }
    }

    // Not needed by this feature, but the two callbacks below have no default implementation.

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
}
