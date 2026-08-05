package com.ariel.diagnostics.leaks

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.fragment.app.FragmentActivity

/**
 * Hands each destroyed Activity to LeakWatcher, and attaches LeakFragmentCallbacks to every Activity
 * able to host fragments.
 */
class LeakActivityCallbacks(
    private val watcher: LeakWatcher,
    private val fragmentCallbacks: LeakFragmentCallbacks,
) : Application.ActivityLifecycleCallbacks {

    // Attaches the fragment callbacks before the Activity's own onCreate, because super.onCreate()
    // is where a FragmentActivity re-creates its fragments.
    override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (activity is FragmentActivity) {
            activity.supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentCallbacks, true)
        }
    }

    // Starts watching the Activity, unless it is only being replaced for a configuration change: the
    // old instance is legitimately still held for a moment while the framework hands its state to
    // the new one, so watching it would report a leak on every rotate.
    override fun onActivityDestroyed(activity: Activity) {
        if (!activity.isChangingConfigurations) {
            watcher.watch(activity, activity.javaClass.simpleName, "Activity")
        }

        // TODO: watch ViewModels and Services here too. A ViewModelStore offers no way to list its
        //  ViewModels without reflection, and nothing tells the library that a Service has been
        //  destroyed, so neither fits what this feature can do today.

        if (activity is FragmentActivity) {
            activity.supportFragmentManager.unregisterFragmentLifecycleCallbacks(fragmentCallbacks)
        }
    }

    // This feature only cares about the moment a component is destroyed, but the six callbacks below
    // have no default implementation.

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
}
