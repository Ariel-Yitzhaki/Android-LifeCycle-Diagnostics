package com.ariel.diagnostics.blocking

import android.view.Window
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

/**
 * Tells ForegroundScreenTracker which fragment is in front, so findings name a screen rather than
 * the one Activity a whole app can be built inside, and cuts the two per screen counters at the
 * same moments, so each fragment's frames and main thread time are reported on their own.
 *
 * The start and stop callbacks are only here for DialogFragments, which have a window of their own.
 * Every other fragment draws into its Activity's window, which BlockingActivityCallbacks already
 * counts.
 */
class BlockingFragmentCallbacks(
    private val foregroundScreen: ForegroundScreenTracker,
    private val jankTracker: JankTracker,
    private val busyTracker: MainThreadBusyTracker,
) : FragmentManager.FragmentLifecycleCallbacks() {

    // JankStats watches one window, so without a counter of its own a dialog's frames would be
    // counted nowhere: they are not drawn into the Activity's window.
    override fun onFragmentStarted(fm: FragmentManager, f: Fragment) {
        val window = dialogWindowOf(f)
        if (window != null) {
            jankTracker.startTracking(f, window, f.javaClass.simpleName)
        }
    }

    // The tracker is always told first, since both counters below read the new answer from it.
    override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
        foregroundScreen.onFragmentResumed(f.javaClass.simpleName)
        jankTracker.onForegroundScreenChanged()
        busyTracker.onForegroundScreenChanged()
    }

    override fun onFragmentPaused(fm: FragmentManager, f: Fragment) {
        foregroundScreen.onFragmentPaused(f.javaClass.simpleName)
        jankTracker.onForegroundScreenChanged()
        busyTracker.onForegroundScreenChanged()
    }

    // Unconditional, unlike onFragmentStarted above: stopTracking does nothing for a fragment that
    // was never counted, and asking the fragment for its dialog again here would miss one that has
    // already let go of it.
    override fun onFragmentStopped(fm: FragmentManager, f: Fragment) {
        jankTracker.stopTracking(f)
    }

    // The window a DialogFragment is showing in, or null for every other fragment. Null too for a
    // DialogFragment being used as an ordinary fragment, which is what setShowsDialog(false) does,
    // and for one whose dialog has not been created yet.
    private fun dialogWindowOf(f: Fragment): Window? {
        if (f !is DialogFragment) {
            return null
        }
        return f.dialog?.window
    }
}
