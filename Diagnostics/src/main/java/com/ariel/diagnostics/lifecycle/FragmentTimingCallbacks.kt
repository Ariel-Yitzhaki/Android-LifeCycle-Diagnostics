package com.ariel.diagnostics.lifecycle

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

/**
 * Times Fragment lifecycle callbacks, the same six as for Activities.
 *
 * Fragments are weaker than Activities here: only onCreate has a real before/after pair
 * (onFragmentPreCreated + onFragmentCreated). Every other callback fires *after* the fragment's own
 * code has already run, so for those the measurement is the gap since the previous callback we saw
 * and is marked approximate — it contains the callback plus whatever the framework did on either
 * side of it.
 */
class FragmentTimingCallbacks(
    private val seenScreens: SeenScreens,
    private val logger: TimingLogger,
) : FragmentManager.FragmentLifecycleCallbacks() {

    // When each fragment's clock was last started. Same reasoning as in ActivityTimingCallbacks:
    // the key must be the Fragment instance and not the class name, because one screen very often
    // has several live fragments of the same class at once — two pages of a ViewPager, a list and
    // its detail pane, or the same fragment pushed onto the back stack twice. Keying by class name
    // would let one of them overwrite another's start time.
    //
    // Fragment does not override equals/hashCode, so a plain HashMap compares keys by identity.
    //
    // An entry is created when the fragment is created and removed in onFragmentDestroyed, so no
    // fragment is held past its own death and the map cannot grow over a long session.
    private val startTimes = HashMap<Fragment, Long>()

    // ---- The one exact measurement -------------------------------------------------------------

    override fun onFragmentPreCreated(fm: FragmentManager, f: Fragment, savedInstanceState: Bundle?) {
        mark(f)
    }

    override fun onFragmentCreated(fm: FragmentManager, f: Fragment, savedInstanceState: Bundle?) {
        // The only fragment callback with a real "before" half, so the only exact one. It does
        // include any child fragments this fragment creates, because the framework creates them
        // inside the same step.
        record(f, "onCreate", approximate = false)
    }

    // ---- Not measured, but it restarts the clock -----------------------------------------------

    override fun onFragmentViewCreated(
        fm: FragmentManager,
        f: Fragment,
        v: View,
        savedInstanceState: Bundle?,
    ) {
        // Nothing is reported here: onCreateView/onViewCreated are not among the six callbacks this
        // feature measures. The clock is restarted anyway so that the onStart gap below does not
        // also contain layout inflation, which would swamp it and make onStart look guilty.
        mark(f)
    }

    // ---- The approximate ones ------------------------------------------------------------------

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
        // Last callback this class handles for this fragment, so drop its clock. record() above
        // restarted it and nobody will ever stop it. (onFragmentDetached does fire after this one,
        // but it is not one of the six callbacks we measure.)
        startTimes.remove(f)
    }

    // ---- The actual measuring ------------------------------------------------------------------

    /** Starts (or restarts) the clock for [fragment]. */
    private fun mark(fragment: Fragment) {
        // Monotonic clock, same as in ActivityTimingCallbacks — only differences of it mean anything.
        startTimes[fragment] = System.nanoTime()
    }

    /**
     * Prints one measurement for [fragment] and then restarts its clock, so that the next callback
     * is measured from here. Does nothing when there is no start time, which happens when the
     * library is installed while a fragment is already part-way through its lifecycle.
     */
    private fun record(fragment: Fragment, callbackName: String, approximate: Boolean) {
        // Read the clock first, before any of our own bookkeeping.
        val endNanos = System.nanoTime()

        val startNanos = startTimes[fragment]
        if (startNanos == null) {
            // No start time. Still start one, so the next callback for this fragment can be
            // measured even though this one could not.
            mark(fragment)
            return
        }

        // A fragment has no isChangingConfigurations of its own, so ask the Activity hosting it.
        // The host is already null by the time some of the late callbacks arrive, and "no host" is
        // not a configuration change, so a missing Activity becomes false.
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

        // Restart the clock from now, so the next callback's gap starts where this one ended
        // instead of overlapping it.
        mark(fragment)
    }
}
