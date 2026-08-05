package com.ariel.diagnostics.leaks

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

/**
 * Hands each destroyed Fragment and Fragment view to LeakWatcher. The two are watched separately
 * because they have separate lives and can leak independently.
 */
class LeakFragmentCallbacks(
    private val watcher: LeakWatcher,
) : FragmentManager.FragmentLifecycleCallbacks() {

    // The last callback at which the view can be reached: the FragmentManager clears the fragment's
    // view field only after this returns.
    override fun onFragmentViewDestroyed(fm: FragmentManager, f: Fragment) {
        val view = f.view
        if (view != null) {
            // Reported under the fragment's class name, because the view class is usually a
            // ConstraintLayout shared by half the app.
            watcher.watch(view, f.javaClass.simpleName, "Fragment view")
        }
    }

    // Includes the fragments a rotation destroys: unlike an Activity, a fragment is handed nothing
    // by its replacement, so there is no built-in reason for the old instance to still be
    // referenced afterwards.
    override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
        watcher.watch(f, f.javaClass.simpleName, "Fragment")
    }
}
