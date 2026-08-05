package com.ariel.diagnostics.lifecycle

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

/**
 * Times Fragment lifecycle callbacks, the same six as for Activities, plus the work of building the
 * fragment's view, which is where most of the cost of a screen built out of Views actually sits.
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

    // The clock for view creation, kept apart from the one above because the two do not start at
    // the same moment and only one of them can be running for a fragment that has no view.
    //
    // An entry is added when a fragment's own onCreate returns and taken out again when its view
    // exists, so a fragment only ever has one while it is on its way to a first view.
    private val viewCreationStartTimes = HashMap<Fragment, Long>()

    override fun onFragmentPreCreated(fm: FragmentManager, f: Fragment, savedInstanceState: Bundle?) {
        mark(f)
    }

    // The only fragment callback with a real "before" half, so the only exact measurement.
    override fun onFragmentCreated(fm: FragmentManager, f: Fragment, savedInstanceState: Bundle?) {
        record(f, "onCreate", MeasurementKind.EXACT)
        // The framework moves a fragment from created to view created in one pass, so this is the
        // closest thing to a before half that view creation has.
        viewCreationStartTimes[f] = System.nanoTime()
    }

    // Runs after both onCreateView and onViewCreated, so the gap since onCreate is the whole cost of
    // putting the fragment's view together.
    override fun onFragmentViewCreated(
        fm: FragmentManager,
        f: Fragment,
        v: View,
        savedInstanceState: Bundle?,
    ) {
        recordViewCreation(f)
        // Restarted here rather than left running, so the onStart gap does not contain the view
        // work that was just reported on its own.
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
        // Last callback this class handles for the fragment, so drop both clocks. The view one is
        // still running for a fragment that never had a view, such as a headless one whose
        // onCreateView returned null.
        startTimes.remove(f)
        viewCreationStartTimes.remove(f)
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

        log(fragment, callbackName, endNanos - startNanos, kind)

        mark(fragment)
    }

    // Prints what it cost to build the fragment's view, and never restarts its own clock: a fragment
    // gets one measurement per view it is given, not a running total.
    private fun recordViewCreation(fragment: Fragment) {
        val endNanos = System.nanoTime()

        val startNanos = viewCreationStartTimes.remove(fragment)
        if (startNanos == null) {
            // A fragment coming back from the back stack keeps its instance and is only given a new
            // view, so its onCreate did not run again and there is nothing to measure from. The
            // last thing this class timed for it was its onStop, which can be minutes ago on a
            // screen the user was reading, so reporting that gap would be worse than saying
            // nothing.
            return
        }

        log(fragment, "onCreateView", endNanos - startNanos, MeasurementKind.VIEW_CREATION)
    }

    // Builds and prints one measurement. The callers above differ only in where the duration and
    // the kind came from.
    private fun log(
        fragment: Fragment,
        callbackName: String,
        durationNanos: Long,
        kind: MeasurementKind,
    ) {
        // A fragment has no isChangingConfigurations of its own, and the host is already null by
        // the time some of the late callbacks arrive.
        val activity = fragment.activity
        val configurationChange = if (activity == null) false else activity.isChangingConfigurations

        val screenName = fragment.javaClass.simpleName
        val timing = LifecycleTiming(
            screenName = screenName,
            callbackName = callbackName,
            durationNanos = durationNanos,
            firstSeen = seenScreens.isFirstTime(screenName),
            configurationChange = configurationChange,
            kind = kind,
        )
        logger.log(timing)
    }
}
