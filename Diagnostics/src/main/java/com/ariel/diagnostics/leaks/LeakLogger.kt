package com.ariel.diagnostics.leaks

import android.util.Log

/** Prints this feature's results to Logcat, one line each. */
class LeakLogger {

    // Prints one ordinary line at debug level, such as the startup line printed at install time.
    //
    // This feature would otherwise say nothing at all in a healthy app, and a tag that has never
    // been printed under does not exist as far as Logcat is concerned: it cannot be filtered on and
    // Android Studio does not offer it. Silence would then look the same as the feature being off.
    fun note(message: String) {
        Log.d(LeakConstants.LOG_TAG, message)
    }

    fun report(record: ScreenLeakRecord) {
        Log.w(
            LeakConstants.LOG_TAG,
            "${record.screenName} (${label(record.kind)}) was still in memory " +
                "${record.retainedCount} of ${record.destroyedCount} times it was destroyed this " +
                "session. ${cause(record.kind)}",
        )
    }

    // The name a kind is printed under.
    private fun label(kind: WatchedKind): String {
        return when (kind) {
            WatchedKind.ACTIVITY -> "Activity"
            WatchedKind.FRAGMENT -> "Fragment"
            WatchedKind.FRAGMENT_VIEW -> "Fragment view"
            WatchedKind.VIEW_OF_LIVE_FRAGMENT -> "Fragment view, fragment still alive"
        }
    }

    // Where to go and look, which is not the same question for a view a fragment is still holding
    // as it is for a whole screen. The library can only say that something is holding the
    // component, never what, so both answers name the usual suspects rather than guessing.
    private fun cause(kind: WatchedKind): String {
        if (kind == WatchedKind.VIEW_OF_LIVE_FRAGMENT) {
            return "The fragment outlived this view, which is what happens to every fragment put " +
                "on the back stack, so the usual cause is one of the fragment's own fields still " +
                "pointing at it: a binding or a findViewById result that onDestroyView did not " +
                "clear"
        }
        return "The usual causes are a static or companion object field still pointing at it, a " +
            "listener or receiver that was registered and never unregistered, and an inner class, " +
            "Handler or Runnable that outlived the screen"
    }
}
