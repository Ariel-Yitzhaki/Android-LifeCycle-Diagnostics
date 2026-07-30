package com.ariel.diagnostics.callbacks

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

/**
 * Listens to Fragment lifecycle callbacks and passes each one on to [TransitionTracker], the same
 * way [ActivityValidationCallbacks] does for Activities.
 *
 * Fragments have two callbacks Activities do not: their view can be created and destroyed
 * separately from the fragment itself, several times over. Those two are counted rather than
 * treated as lifecycle states.
 *
 * One instance of this class is shared by every Activity in the process;
 * [ActivityValidationCallbacks] attaches it to each Activity's FragmentManager and detaches it
 * again on destroy.
 */
class FragmentValidationCallbacks(
    private val tracker: TransitionTracker,
) : FragmentManager.FragmentLifecycleCallbacks() {

    /**
     * Runs after a Fragment's own onCreate. Called by the FragmentManager once per Fragment
     * instance. This is where that instance's record is first created.
     */
    override fun onFragmentCreated(fm: FragmentManager, f: Fragment, savedInstanceState: Bundle?) {
        tracker.onEvent(f, f.javaClass.simpleName, "Fragment", "onCreate", LifecycleState.CREATED)
    }

    /**
     * Runs after a Fragment's view has been built and onViewCreated has finished. Called by the
     * FragmentManager every time the fragment is given a view — which can happen more than once for
     * the same fragment, because a fragment on the back stack loses its view and gets a new one
     * when it comes back.
     */
    override fun onFragmentViewCreated(
        fm: FragmentManager,
        f: Fragment,
        v: View,
        savedInstanceState: Bundle?,
    ) {
        tracker.onFragmentViewCreated(f, f.javaClass.simpleName)
    }

    /** Runs after a Fragment's own onStart. Called by the FragmentManager every time it becomes visible. */
    override fun onFragmentStarted(fm: FragmentManager, f: Fragment) {
        tracker.onEvent(f, f.javaClass.simpleName, "Fragment", "onStart", LifecycleState.STARTED)
    }

    /** Runs after a Fragment's own onResume. Called by the FragmentManager every time it reaches the foreground. */
    override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
        tracker.onEvent(f, f.javaClass.simpleName, "Fragment", "onResume", LifecycleState.RESUMED)
    }

    /** Runs after a Fragment's own onPause. Called by the FragmentManager as it leaves the foreground. */
    override fun onFragmentPaused(fm: FragmentManager, f: Fragment) {
        tracker.onEvent(f, f.javaClass.simpleName, "Fragment", "onPause", LifecycleState.PAUSED)
    }

    /** Runs after a Fragment's own onStop. Called by the FragmentManager every time it stops being visible. */
    override fun onFragmentStopped(fm: FragmentManager, f: Fragment) {
        tracker.onEvent(f, f.javaClass.simpleName, "Fragment", "onStop", LifecycleState.STOPPED)
    }

    /**
     * Runs after a Fragment's own onDestroyView. Called by the FragmentManager every time the
     * fragment's view is torn down, which happens whenever the fragment goes onto the back stack —
     * not only when the fragment itself is going away.
     *
     * This is the callback the missing-onDestroyView check counts. A fragment that keeps a
     * reference to its view, or to anything inside it, has to let go of it here.
     */
    override fun onFragmentViewDestroyed(fm: FragmentManager, f: Fragment) {
        tracker.onFragmentViewDestroyed(f, f.javaClass.simpleName)
    }

    /**
     * Runs after a Fragment's own onDestroy. Called by the FragmentManager once per Fragment
     * instance, and it is the last of the six lifecycle callbacks that instance will produce, so
     * this is where its end-of-life checks happen.
     */
    override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
        tracker.onFragmentDestroyed(f, f.javaClass.simpleName)
    }
}
