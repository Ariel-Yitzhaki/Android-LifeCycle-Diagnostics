package com.ariel.diagnostics.leaks

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

/**
 * Listens for Fragments and for Fragment views being destroyed, and hands each one to
 * [LeakWatcher].
 *
 * A fragment and the view it puts on screen are watched as two separate things, because they have
 * separate lives and can leak separately.
 *
 * One instance of this class is shared by every Activity in the process; [LeakActivityCallbacks]
 * attaches it to each Activity's FragmentManager and detaches it again on destroy.
 */
class LeakFragmentCallbacks(
    private val watcher: LeakWatcher,
) : FragmentManager.FragmentLifecycleCallbacks() {

    /**
     * Runs after a Fragment's own onDestroyView. Called by the FragmentManager every time a
     * fragment's view is torn down, which happens whenever the fragment goes onto the back stack —
     * not only when the fragment itself is going away.
     *
     * The view is watched here, separately from the fragment that owns it, because the two leak
     * independently. A fragment can be collected perfectly well and still leave its entire view
     * tree behind, held by a binding field, an adapter or a listener that was never cleared in
     * onDestroyView. That is also the more common of the two mistakes to make: a fragment sitting
     * on the back stack is *supposed* to outlive its view, so nothing looks wrong from the outside
     * until the abandoned view trees pile up.
     */
    override fun onFragmentViewDestroyed(fm: FragmentManager, f: Fragment) {
        // The fragment still points at its view at this exact moment: the FragmentManager sends
        // this callback first and only clears the fragment's own view field afterwards. This is the
        // last callback at which the view can be reached at all, which is why the watching happens
        // here and not in onFragmentDestroyed.
        val view = f.view
        if (view != null) {
            // Reported under the fragment's class name rather than the view's own, because "the
            // view of FragmentViewLeakFragment" is something a reviewer can act on, while the view
            // class is usually a ConstraintLayout shared by half the app.
            watcher.watch(view, f.javaClass.simpleName, "Fragment view")
        }
    }

    /**
     * Runs after a Fragment's own onDestroy. Called by the FragmentManager once per Fragment
     * instance, and it is the last callback that instance will ever produce, so this is where the
     * fragment itself starts being watched.
     */
    override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
        // Every destroyed fragment is watched, including the ones a rotation destroys. Unlike an
        // Activity, a fragment is handed nothing by its replacement, so there is no built-in reason
        // for the old instance to still be referenced afterwards. If rotations do turn out to
        // produce false alarms here, the check to add is f.activity?.isChangingConfigurations, the
        // same exclusion LeakActivityCallbacks makes for Activities.
        watcher.watch(f, f.javaClass.simpleName, "Fragment")
    }
}
