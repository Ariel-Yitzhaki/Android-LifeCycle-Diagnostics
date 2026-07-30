package com.ariel.diagnostics.leaks

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.fragment.app.FragmentActivity

/**
 * Listens for Activities being destroyed and hands each one to [LeakWatcher], and attaches
 * [LeakFragmentCallbacks] to every Activity that is able to host fragments.
 *
 * This is a third, separate registration alongside Feature 1's and Feature 2's. The framework keeps
 * a list of these and calls each one in turn, so the three features never touch.
 */
class LeakActivityCallbacks(
    private val watcher: LeakWatcher,
    private val fragmentCallbacks: LeakFragmentCallbacks,
) : Application.ActivityLifecycleCallbacks {

    /**
     * Runs immediately before an Activity's own onCreate. Called by the framework for every
     * Activity in this process.
     *
     * The fragment callbacks are attached this early because super.onCreate() is where a
     * FragmentActivity restores and recreates its fragments. Attaching any later would miss every
     * fragment that was already on the screen by then.
     */
    override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (activity is FragmentActivity) {
            // Only a FragmentActivity has a supportFragmentManager. The second argument is
            // "recursive": true also covers fragments nested inside other fragments, which is what
            // a ViewPager or a nested navigation graph produces.
            activity.supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentCallbacks, true)
        }
    }

    /**
     * Runs after an Activity's own onDestroy. Called by the framework once per Activity instance,
     * and it is the last callback that instance will ever produce — so it is the moment from which
     * nothing in the app has any business still holding it.
     *
     * This is where an Activity starts being watched, and it is the only place in this feature that
     * skips a component on purpose.
     */
    override fun onActivityDestroyed(activity: Activity) {
        // isChangingConfigurations is true when this Activity is not really going away: a rotation,
        // a theme switch or a font-size change destroys the Activity and immediately builds a
        // replacement, by design. Around that swap the old instance is legitimately still held for
        // a moment while the framework hands its state to the new one, so watching it would report
        // a leak for something Android does deliberately on every single rotate.
        if (!activity.isChangingConfigurations) {
            watcher.watch(activity, activity.javaClass.simpleName, "Activity")
        }

        // TODO: watch ViewModels and Services here too. Neither fits what this feature can do
        //  today. A ViewModel is held by a ViewModelStore that keeps its ViewModels in an internal
        //  map and offers no way to list them, so reaching them would need reflection, which this
        //  library does not use. A Service has no equivalent of registerActivityLifecycleCallbacks
        //  — nothing tells the library that one has been destroyed — so there is no moment at which
        //  watching could even start. Both are out for now.

        if (activity is FragmentActivity) {
            // The FragmentManager is thrown away with the Activity, so this is not strictly needed,
            // but unregistering means the library never depends on that being true. Every fragment
            // in this Activity has already been destroyed and reported by this point, because
            // FragmentActivity tears its fragments down inside its own onDestroy.
            activity.supportFragmentManager.unregisterFragmentLifecycleCallbacks(fragmentCallbacks)
        }
    }

    // The six callbacks below are the rest of the interface. Kotlin makes us implement them because
    // they have no default implementation, but this feature only cares about the moment a component
    // is destroyed, so none of them has anything to do. They are left empty rather than removed.

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
}
