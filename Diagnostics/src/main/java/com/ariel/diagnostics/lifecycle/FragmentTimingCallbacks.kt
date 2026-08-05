package com.ariel.diagnostics.lifecycle

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

/**
 * Times Fragment lifecycle callbacks, the same six as for Activities.
 *
 * Only onCreate has a real before/after pair. Every other callback fires after the fragment's own
 * code has run, so those measurements are the gap since the previous callback. See [MeasurementKind]
 * for what each of those gaps actually contains, and note that the onResume to onPause gap is user
 * idle time rather than work.
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
        record(f, "onCreate", MeasurementKind.EXACT)
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
        record(f, "onStart", MeasurementKind.BETWEEN_CALLBACKS)
    }

    override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
        record(f, "onResume", MeasurementKind.BETWEEN_CALLBACKS)
    }

    // The clock has been running since onResume, and nothing happens between a fragment resuming
    // and pausing except the user looking at it. So this is time on screen, not the cost of
    // onPause, and reporting it as a callback duration would flag every screen the user reads for
    // more than a moment as slow.
    override fun onFragmentPaused(fm: FragmentManager, f: Fragment) {
        record(f, "onPause", MeasurementKind.TIME_ON_SCREEN)
    }

    override fun onFragmentStopped(fm: FragmentManager, f: Fragment) {
        record(f, "onStop", MeasurementKind.BETWEEN_CALLBACKS)
    }

    // The gap here often spans the incoming fragment's onCreate and view inflation, because the
    // framework interleaves one fragment's teardown with the next one's setup.
    override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
        record(f, "onDestroy", MeasurementKind.BETWEEN_CALLBACKS)
        // Last callback this class handles for the fragment, so drop the clock record() restarted.
        startTimes.remove(f)
    }

    private fun mark(fragment: Fragment) {
        startTimes[fragment] = System.nanoTime()
    }

    // Prints one measurement for the fragment and restarts its clock. Does nothing but restart when
    // there is no start time, which happens when the library is installed part-way through a
    // fragment's lifecycle.
    private fun record(fragment: Fragment, callbackName: String, kind: MeasurementKind) {
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
            kind = kind,
        )
        logger.log(timing)

        mark(fragment)
    }
}
