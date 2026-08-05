package com.ariel.diagnostics.callbacks

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.fragment.app.FragmentActivity

/**
 * Passes every Activity lifecycle callback on to TransitionTracker, and the resume/pause steps to
 * StrictModeWatcher as well so it knows which screen is in front. Also installs the StrictMode
 * policy and attaches FragmentValidationCallbacks to every Activity able to host fragments.
 */
class ActivityValidationCallbacks(
    private val tracker: TransitionTracker,
    private val strictModeWatcher: StrictModeWatcher,
    private val fragmentCallbacks: FragmentValidationCallbacks,
) : Application.ActivityLifecycleCallbacks {

    // Runs immediately before an Activity's own onCreate — after the app's Application.onCreate has
    // finished but before any screen exists, which is the only moment both the StrictMode policy and
    // the fragment callbacks can be installed. See StrictModeWatcher.installIfNeeded.
    override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
        strictModeWatcher.installIfNeeded(activity)

        if (activity is FragmentActivity) {
            activity.supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentCallbacks, true)
        }
    }

    // Runs after an Activity's own onCreate. This is where that instance's record is first created.
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        tracker.onEvent(
            activity,
            activity.javaClass.simpleName,
            "Activity",
            "onCreate",
            LifecycleState.CREATED,
        )
    }

    // Runs after an Activity's own onStart. The tracker counts these so it can check at the end that
    // they balance against the onStops.
    override fun onActivityStarted(activity: Activity) {
        tracker.onEvent(
            activity,
            activity.javaClass.simpleName,
            "Activity",
            "onStart",
            LifecycleState.STARTED,
        )
    }

    // Runs after an Activity's own onResume, also the newest answer to which screen is in front.
    override fun onActivityResumed(activity: Activity) {
        strictModeWatcher.onActivityResumed(activity.javaClass.simpleName)
        tracker.onEvent(
            activity,
            activity.javaClass.simpleName,
            "Activity",
            "onResume",
            LifecycleState.RESUMED,
        )
    }

    // Runs after an Activity's own onPause, always before the next Activity is resumed, so clearing
    // the foreground name here cannot wipe out a newer one.
    override fun onActivityPaused(activity: Activity) {
        strictModeWatcher.onActivityPaused()
        tracker.onEvent(
            activity,
            activity.javaClass.simpleName,
            "Activity",
            "onPause",
            LifecycleState.PAUSED,
        )
    }

    // Runs after an Activity's own onStop. Counted against the onStarts above; an Activity that dies
    // with the two unequal has missed a callback or taken them out of order.
    override fun onActivityStopped(activity: Activity) {
        tracker.onEvent(
            activity,
            activity.javaClass.simpleName,
            "Activity",
            "onStop",
            LifecycleState.STOPPED,
        )
    }

    // Runs after an Activity's own onDestroy, so this is where its end-of-life checks happen.
    override fun onActivityDestroyed(activity: Activity) {
        // isChangingConfigurations keeps rotations out of the restart-loop count.
        tracker.onActivityDestroyed(
            activity,
            activity.javaClass.simpleName,
            activity.isChangingConfigurations,
        )

        if (activity is FragmentActivity) {
            activity.supportFragmentManager.unregisterFragmentLifecycleCallbacks(fragmentCallbacks)
        }
    }

    // Saving state is not a lifecycle step and does not change which state the Activity is in, but
    // the interface has no default implementation for it.
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
}
