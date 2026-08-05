package com.ariel.diagnostics.lifecycle

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.fragment.app.FragmentActivity

/**
 * Times the app's own code inside each Activity lifecycle callback using the Pre/Post pairs on
 * Application.ActivityLifecycleCallbacks, and attaches FragmentTimingCallbacks to every Activity
 * that can host fragments.
 */
class ActivityTimingCallbacks(
    private val seenScreens: SeenScreens,
    private val logger: TimingLogger,
    private val fragmentCallbacks: FragmentTimingCallbacks,
) : Application.ActivityLifecycleCallbacks {

    // Keyed by instance, not class name: two Activities of the same class can be alive at once.
    // Every Pre callback adds one entry and the matching Post callback removes it.
    private val startTimes = HashMap<Activity, Long>()

    // Registers the fragment callbacks before the Activity's own onCreate, because super.onCreate()
    // is where a FragmentActivity re-creates its fragments.
    override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (activity is FragmentActivity) {
            activity.supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentCallbacks, true)
        }
        // Marked last so the registration above is not counted as part of the app's onCreate.
        markStart(activity)
    }

    // Reports onCreate, which for a fragment host also contains its fragments' onCreate.
    override fun onActivityPostCreated(activity: Activity, savedInstanceState: Bundle?) {
        recordEnd(activity, "onCreate")
    }

    override fun onActivityPreStarted(activity: Activity) {
        markStart(activity)
    }

    override fun onActivityPostStarted(activity: Activity) {
        recordEnd(activity, "onStart")
    }

    override fun onActivityPreResumed(activity: Activity) {
        markStart(activity)
    }

    override fun onActivityPostResumed(activity: Activity) {
        recordEnd(activity, "onResume")
    }

    override fun onActivityPrePaused(activity: Activity) {
        markStart(activity)
    }

    override fun onActivityPostPaused(activity: Activity) {
        recordEnd(activity, "onPause")
    }

    override fun onActivityPreStopped(activity: Activity) {
        markStart(activity)
    }

    override fun onActivityPostStopped(activity: Activity) {
        recordEnd(activity, "onStop")
    }

    override fun onActivityPreDestroyed(activity: Activity) {
        markStart(activity)
    }

    override fun onActivityPostDestroyed(activity: Activity) {
        recordEnd(activity, "onDestroy")
        if (activity is FragmentActivity) {
            activity.supportFragmentManager.unregisterFragmentLifecycleCallbacks(fragmentCallbacks)
        }
    }

    // The seven callbacks below have no default implementation, so they must be overridden. The
    // Pre/Post pairs above already cover the same moments.

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}

    private fun markStart(activity: Activity) {
        // nanoTime is monotonic, so it never jumps when the wall clock is corrected.
        startTimes[activity] = System.nanoTime()
    }

    // Prints one measurement for the Activity. Does nothing when there is no start time, which
    // happens when the library is installed part-way through a callback pair.
    private fun recordEnd(activity: Activity, callbackName: String) {
        val endNanos = System.nanoTime()

        val startNanos = startTimes.remove(activity)
        if (startNanos == null) {
            return
        }

        val screenName = activity.javaClass.simpleName
        val timing = LifecycleTiming(
            screenName = screenName,
            callbackName = callbackName,
            durationNanos = endNanos - startNanos,
            firstSeen = seenScreens.isFirstTime(screenName),
            // Only ever true on the way out: Android sets it on the instance about to be replaced.
            configurationChange = activity.isChangingConfigurations,
            approximate = false,
        )
        logger.log(timing)
    }
}
