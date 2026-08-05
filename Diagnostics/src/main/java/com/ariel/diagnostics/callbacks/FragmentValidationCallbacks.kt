package com.ariel.diagnostics.callbacks

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

/**
 * Passes Fragment lifecycle callbacks on to TransitionTracker, the way ActivityValidationCallbacks
 * does for Activities. View creation and destruction are counted rather than treated as lifecycle
 * states, because they can happen several times per fragment.
 */
class FragmentValidationCallbacks(
    private val tracker: TransitionTracker,
) : FragmentManager.FragmentLifecycleCallbacks() {

    // Runs after a Fragment's own onCreate. This is where that instance's record is first created.
    override fun onFragmentCreated(fm: FragmentManager, f: Fragment, savedInstanceState: Bundle?) {
        tracker.onEvent(f, f.javaClass.simpleName, "Fragment", "onCreate", LifecycleState.CREATED)
    }

    // Can happen more than once for the same fragment, because a fragment on the back stack loses
    // its view and gets a new one when it comes back.
    override fun onFragmentViewCreated(
        fm: FragmentManager,
        f: Fragment,
        v: View,
        savedInstanceState: Bundle?,
    ) {
        tracker.onFragmentViewCreated(f, f.javaClass.simpleName)
    }

    override fun onFragmentStarted(fm: FragmentManager, f: Fragment) {
        tracker.onEvent(f, f.javaClass.simpleName, "Fragment", "onStart", LifecycleState.STARTED)
    }

    override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
        tracker.onEvent(f, f.javaClass.simpleName, "Fragment", "onResume", LifecycleState.RESUMED)
    }

    override fun onFragmentPaused(fm: FragmentManager, f: Fragment) {
        tracker.onEvent(f, f.javaClass.simpleName, "Fragment", "onPause", LifecycleState.PAUSED)
    }

    override fun onFragmentStopped(fm: FragmentManager, f: Fragment) {
        tracker.onEvent(f, f.javaClass.simpleName, "Fragment", "onStop", LifecycleState.STOPPED)
    }

    // Runs after a Fragment's own onDestroyView, which happens whenever the fragment goes onto the
    // back stack and not only when the fragment itself is going away.
    override fun onFragmentViewDestroyed(fm: FragmentManager, f: Fragment) {
        tracker.onFragmentViewDestroyed(f, f.javaClass.simpleName)
    }

    // Runs after a Fragment's own onDestroy, so this is where its end-of-life checks happen.
    override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
        tracker.onFragmentDestroyed(f, f.javaClass.simpleName)
    }
}
