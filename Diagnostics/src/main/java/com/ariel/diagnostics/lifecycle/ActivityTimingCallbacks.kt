package com.ariel.diagnostics.lifecycle

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.fragment.app.FragmentActivity

/**
 * Times the developer's own code inside each Activity lifecycle callback.
 *
 * It uses the Pre/Post pairs on Application.ActivityLifecycleCallbacks: the framework calls the Pre
 * half immediately before the Activity's own onCreate/onStart/... runs and the Post half immediately
 * after it returns, so the difference between the two is that Activity's own work. It also attaches
 * [FragmentTimingCallbacks] to every Activity that can host fragments.
 */
class ActivityTimingCallbacks(
    private val seenScreens: SeenScreens,
    private val logger: TimingLogger,
    private val fragmentCallbacks: FragmentTimingCallbacks,
) : Application.ActivityLifecycleCallbacks {

    // Start timestamps, one entry per live Activity object.
    //
    // The key has to be the instance and not the class name, because two Activities of the same
    // class are alive at the same time in perfectly normal situations: during a rotation the
    // replacement instance is created before the old one is destroyed, and the same screen can sit
    // on the back stack twice. A class-keyed entry would be overwritten by the second instance and
    // the first instance's next Post callback would compute its duration from the wrong start time.
    //
    // Activity does not override equals/hashCode, so a plain HashMap already compares keys by
    // identity, which is exactly what "per instance" means here.
    //
    // Nothing accumulates: every Pre callback puts one entry and the matching Post callback removes
    // it, so the map is empty between callbacks and never holds an Activity alive.
    private val startTimes = HashMap<Activity, Long>()

    // ---- onCreate ----------------------------------------------------------------------------

    override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
        // Fragment callbacks are registered here, before the Activity's own onCreate runs, because
        // super.onCreate() is where a FragmentActivity restores and re-creates its fragments.
        // Registering any later would miss the first onCreate of every fragment on the screen.
        if (activity is FragmentActivity) {
            // The second argument is "recursive": true also covers fragments nested inside other
            // fragments, which is what a ViewPager or a nested navigation graph produces.
            activity.supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentCallbacks, true)
        }
        // Marked last so the registration above is not counted as part of the developer's onCreate.
        markStart(activity)
    }

    override fun onActivityPostCreated(activity: Activity, savedInstanceState: Bundle?) {
        // An Activity that hosts fragments drives their matching callback from inside its own, so
        // this duration contains its fragments' onCreate as well. The same is true of onStart,
        // onResume and the rest. That is the honest number for "how long did this callback block
        // the main thread" — the per-fragment lines printed alongside it show where the time went.
        recordEnd(activity, "onCreate")
    }

    // ---- onStart -----------------------------------------------------------------------------

    override fun onActivityPreStarted(activity: Activity) {
        markStart(activity)
    }

    override fun onActivityPostStarted(activity: Activity) {
        recordEnd(activity, "onStart")
    }

    // ---- onResume ----------------------------------------------------------------------------

    override fun onActivityPreResumed(activity: Activity) {
        markStart(activity)
    }

    override fun onActivityPostResumed(activity: Activity) {
        recordEnd(activity, "onResume")
    }

    // ---- onPause -----------------------------------------------------------------------------

    override fun onActivityPrePaused(activity: Activity) {
        markStart(activity)
    }

    override fun onActivityPostPaused(activity: Activity) {
        recordEnd(activity, "onPause")
    }

    // ---- onStop ------------------------------------------------------------------------------

    override fun onActivityPreStopped(activity: Activity) {
        markStart(activity)
    }

    override fun onActivityPostStopped(activity: Activity) {
        recordEnd(activity, "onStop")
    }

    // ---- onDestroy ---------------------------------------------------------------------------

    override fun onActivityPreDestroyed(activity: Activity) {
        markStart(activity)
    }

    override fun onActivityPostDestroyed(activity: Activity) {
        recordEnd(activity, "onDestroy")
        // The FragmentManager is thrown away with the Activity, so this is not strictly needed, but
        // unregistering means the library never depends on that being true.
        if (activity is FragmentActivity) {
            activity.supportFragmentManager.unregisterFragmentLifecycleCallbacks(fragmentCallbacks)
        }
    }

    // ---- Required but unused -------------------------------------------------------------------

    // Kotlin makes us implement these seven: they are the original callbacks from before the
    // Pre/Post pairs existed, and unlike the Pre/Post methods they have no default implementation.
    // The Pre/Post pair above already brackets the same moment, so there is nothing to do in them.

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}

    // ---- The actual measuring ------------------------------------------------------------------

    private fun markStart(activity: Activity) {
        // System.nanoTime is the right clock for measuring a duration: it is monotonic, so it never
        // jumps backwards when the wall clock is corrected or the time zone changes. It is not a
        // date — only the difference between two readings of it means anything.
        startTimes[activity] = System.nanoTime()
    }

    private fun recordEnd(activity: Activity, callbackName: String) {
        // Read the clock before any of our own bookkeeping, so the duration is the developer's code
        // and not ours.
        val endNanos = System.nanoTime()

        // remove() reads the start time and clears the entry in one step; there is exactly one
        // pending entry per Activity because a Pre and its Post run back to back on the main thread
        // with only that Activity's own callback in between.
        val startNanos = startTimes.remove(activity)
        if (startNanos == null) {
            // No start time means install() ran part-way through this callback pair, which happens
            // to the very first Activity if the library is installed late. Skip it rather than
            // report a made-up duration.
            return
        }

        val screenName = activity.javaClass.simpleName
        val timing = LifecycleTiming(
            screenName = screenName,
            callbackName = callbackName,
            durationNanos = endNanos - startNanos,
            firstSeen = seenScreens.isFirstTime(screenName),
            // isChangingConfigurations is only ever true on the way out: Android sets it on the
            // instance that is about to be replaced, before its onPause/onStop/onDestroy. On the way
            // in it is always false, so onCreate/onStart/onResume lines never carry this flag even
            // during a rotation.
            configurationChange = activity.isChangingConfigurations,
            // Activities have real Pre/Post pairs, so this is the callback itself and nothing else.
            approximate = false,
        )
        logger.log(timing)
    }
}
