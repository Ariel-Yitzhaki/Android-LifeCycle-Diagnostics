package com.ariel.diagnostics.callbacks

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.fragment.app.FragmentActivity

/**
 * Listens to every Activity lifecycle callback in the process and passes each one on: the lifecycle
 * steps to [TransitionTracker] for Check A, and the resume/pause steps to [StrictModeWatcher] as
 * well, so it always knows which screen is in front.
 *
 * It is also the class that installs the StrictMode policy, at the earliest moment that works, and
 * that attaches [FragmentValidationCallbacks] to every Activity able to host fragments.
 *
 * This is a second, separate registration alongside Feature 1's own callbacks. The framework is
 * happy to hold any number of them and calls each in turn, so the two features never touch.
 */
class ActivityValidationCallbacks(
    private val tracker: TransitionTracker,
    private val strictModeWatcher: StrictModeWatcher,
    private val fragmentCallbacks: FragmentValidationCallbacks,
) : Application.ActivityLifecycleCallbacks {

    /**
     * Runs immediately before an Activity's own onCreate. Called by the framework for every
     * Activity in this process.
     *
     * Two things have to happen this early: the StrictMode policy, which must be installed after
     * the app's Application.onCreate has finished but before any screen exists, and the fragment
     * callbacks, which must be attached before the Activity's super.onCreate() recreates its
     * fragments.
     */
    override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
        // Does nothing after the first Activity; see StrictModeWatcher for why it is called here.
        strictModeWatcher.installIfNeeded(activity)

        if (activity is FragmentActivity) {
            // Only a FragmentActivity has a supportFragmentManager. The second argument is
            // "recursive": true also covers fragments nested inside other fragments, which is what
            // a ViewPager or a nested navigation graph produces.
            activity.supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentCallbacks, true)
        }
    }

    /**
     * Runs after an Activity's own onCreate has finished. Called by the framework, once per
     * Activity instance. This is where that instance's record is first created.
     */
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        tracker.onEvent(
            activity,
            activity.javaClass.simpleName,
            "Activity",
            "onCreate",
            LifecycleState.CREATED,
        )
    }

    /** Runs after an Activity's own onStart. Called by the framework every time it becomes visible. */
    override fun onActivityStarted(activity: Activity) {
        tracker.onEvent(
            activity,
            activity.javaClass.simpleName,
            "Activity",
            "onStart",
            LifecycleState.STARTED,
        )
    }

    /**
     * Runs after an Activity's own onResume. Called by the framework every time it reaches the
     * foreground, which also makes it the newest answer to "which screen is the user on".
     */
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

    /**
     * Runs after an Activity's own onPause. Called by the framework as it leaves the foreground,
     * always before the next Activity is resumed.
     */
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

    /** Runs after an Activity's own onStop. Called by the framework every time it stops being visible. */
    override fun onActivityStopped(activity: Activity) {
        tracker.onEvent(
            activity,
            activity.javaClass.simpleName,
            "Activity",
            "onStop",
            LifecycleState.STOPPED,
        )
    }

    /**
     * Runs after an Activity's own onDestroy. Called by the framework once per Activity instance,
     * and it is the last callback that instance will ever produce, so this is where the end-of-life
     * checks happen.
     */
    override fun onActivityDestroyed(activity: Activity) {
        // isChangingConfigurations is true when this Activity is only going away to be rebuilt for
        // a rotation, a theme switch or a font-size change. It is still readable here, in onDestroy,
        // and it is what keeps rotations out of the restart-loop count.
        tracker.onActivityDestroyed(
            activity,
            activity.javaClass.simpleName,
            activity.isChangingConfigurations,
        )

        if (activity is FragmentActivity) {
            // The FragmentManager dies with the Activity, so this is not strictly needed, but
            // unregistering means the library never depends on that being true.
            activity.supportFragmentManager.unregisterFragmentLifecycleCallbacks(fragmentCallbacks)
        }
    }

    /**
     * Runs when an Activity is asked to save its state. Called by the framework, but this feature
     * has nothing to check here — saving state is not a lifecycle step and does not change which
     * state the Activity is in. Kotlin makes us implement it because the interface has no default
     * for it.
     */
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
}
