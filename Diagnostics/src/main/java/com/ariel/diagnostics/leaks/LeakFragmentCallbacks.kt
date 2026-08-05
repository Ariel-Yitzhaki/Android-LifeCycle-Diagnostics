package com.ariel.diagnostics.leaks

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

/**
 * Hands each destroyed Fragment and Fragment view to LeakWatcher. The two are watched separately
 * because they have separate lives and can leak independently.
 *
 * One instance is shared by every Activity in the process.
 */
class LeakFragmentCallbacks(
    private val watcher: LeakWatcher,
) : FragmentManager.FragmentLifecycleCallbacks() {

    // Watches the fragment's view, which can leak on its own — held by a binding field, an adapter
    // or a listener never cleared in onDestroyView — while the fragment itself is collected fine.
    //
    // This is the last callback at which the view can be reached: the FragmentManager clears the
    // fragment's view field only after this returns.
    override fun onFragmentViewDestroyed(fm: FragmentManager, f: Fragment) {
        val view = f.view
        if (view != null) {
            // Reported under the fragment's class name, because the view class is usually a
            // ConstraintLayout shared by half the app.
            watcher.watch(view, f.javaClass.simpleName, "Fragment view")
        }
    }

    // Watches every destroyed fragment, including the ones a rotation destroys: unlike an Activity,
    // a fragment is handed nothing by its replacement, so there is no built-in reason for the old
    // instance to still be referenced afterwards.
    override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
        watcher.watch(f, f.javaClass.simpleName, "Fragment")
    }
}
