package com.ariel.diagnostics.lifecycle

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

/**
 * Times Fragment lifecycle callbacks, the same six as for Activities.
 *
 * Only onCreate has a real before/after pair. Every other callback fires after the fragment's own
 * code has run, so those measurements are the gap since the previous callback and are marked
 * approximate.
 */
class FragmentTimingCallbacks(
    private val seenScreens: SeenScreens,
    private val logger: TimingLogger,
) : FragmentManager.FragmentLifecycleCallbacks() {

    // Keyed by instance, not class name: one screen often has several live fragments of the same
    // class. Entries are removed in onFragmentDestroyed.
    private val startTimes = HashMap<Fragment, Long>()

    override fun onFragmentPreCreated(fm: FragmentManager, f: Fragment, savedInstanceState: Bundle?) {
        mark(f)
    }

    // The only fragment callback with a real "before" half, so the only exact measurement.
    override fun onFragmentCreated(fm: FragmentManager, f: Fragment, savedInstanceState: Bundle?) {
        record(f, "onCreate", approximate = false)
    }

    // Not reported, since view creation is not one of the six measured callbacks, but the clock is
    // restarted so the onStart gap does not also contain layout inflation.
    override fun onFragmentViewCreated(
        fm: FragmentManager,
        f: Fragment,
        v: View,
        savedInstanceState: Bundle?,
    ) {
        mark(f)
    }

    override fun onFragmentStarted(fm: FragmentManager, f: Fragment) {
        record(f, "onStart", approximate = true)
    }

    override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
        record(f, "onResume", approximate = true)
    }

    override fun onFragmentPaused(fm: FragmentManager, f: Fragment) {
        record(f, "onPause", approximate = true)
    }

    override fun onFragmentStopped(fm: FragmentManager, f: Fragment) {
        record(f, "onStop", approximate = true)
    }

    override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
        record(f, "onDestroy", approximate = true)
        // Last callback this class handles for the fragment, so drop the clock record() restarted.
        startTimes.remove(f)
    }

    private fun mark(fragment: Fragment) {
        startTimes[fragment] = System.nanoTime()
    }

    // Prints one measurement for the fragment and restarts its clock. Does nothing but restart when
    // there is no start time, which happens when the library is installed part-way through a
    // fragment's lifecycle.
    private fun record(fragment: Fragment, callbackName: String, approximate: Boolean) {
        val endNanos = System.nanoTime()

        val startNanos = startTimes[fragment]
        if (startNanos == null) {
            mark(fragment)
            return
        }

        // A fragment has no isChangingConfigurations of its own, and the host is already null by
        // the time some of the late callbacks arrive.
        val activity = fragment.activity
        val configurationChange = if (activity == null) false else activity.isChangingConfigurations

        val screenName = fragment.javaClass.simpleName
        val timing = LifecycleTiming(
            screenName = screenName,
            callbackName = callbackName,
            durationNanos = endNanos - startNanos,
            firstSeen = seenScreens.isFirstTime(screenName),
            configurationChange = configurationChange,
            approximate = approximate,
        )
        logger.log(timing)

        mark(fragment)
    }
}
