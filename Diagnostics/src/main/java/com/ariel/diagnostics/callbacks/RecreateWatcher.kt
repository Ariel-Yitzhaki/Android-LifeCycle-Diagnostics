package com.ariel.diagnostics.callbacks

import android.os.SystemClock

/**
 * Watches for one Activity class being destroyed and recreated over and over in a short time — a
 * screen restarting itself in a loop, a redirect that bounces back, a finish() in the wrong place.
 *
 * Rotations and other configuration changes do exactly the same thing to an Activity, so they are
 * left out before anything is counted; otherwise every device rotation would look like a fault.
 *
 * What it actually counts is destroys, and it treats a burst of them as a burst of destroy/recreate
 * cycles. That is true in practice — a class only gets destroyed repeatedly if something keeps
 * making new instances of it — but the smarter way is to pair each destroy with the create of the
 * next instance of the same class and count only the pairs.
 */
class RecreateWatcher(private val logger: ValidationLogger) {

    // For each Activity class name, when its recent destroys happened.
    //
    // The key is the class name and never an Activity object, because this map lives as long as the
    // process does and holding an Activity in it would keep that Activity and its whole view tree
    // in memory — which is the very thing Check B is looking for. The number of distinct Activity
    // classes in an app is small and fixed, so this map cannot grow without bound.
    //
    // A plain HashMap needs no locking because every lifecycle callback that reaches this class
    // runs on the main thread.
    private val destroyTimes = HashMap<String, MutableList<Long>>()

    /**
     * Records that one instance of [activityName] was just destroyed, and reports thrash if too
     * many of them have been destroyed inside the recent window.
     *
     * Called by [TransitionTracker] from the Activity's onDestroy, once per destroyed Activity.
     * [configurationChange] is true when the Activity is only being replaced because of a rotation
     * or a similar configuration change.
     */
    fun onActivityDestroyed(activityName: String, configurationChange: Boolean) {
        if (configurationChange) {
            // A configuration change destroys and immediately recreates the Activity by design, so
            // it is not evidence of anything. Rotating the screen five times in ten seconds is a
            // person fidgeting, not a bug.
            return
        }

        // elapsedRealtime() counts milliseconds since the device booted and never jumps backwards,
        // unlike currentTimeMillis(), which moves when the clock is corrected or the time zone
        // changes and could make a ten-second window look like an hour or like nothing at all.
        val now = SystemClock.elapsedRealtime()

        // First destroy ever seen for this class, so start its list.
        var times = destroyTimes[activityName]
        if (times == null) {
            times = ArrayList()
            destroyTimes[activityName] = times
        }
        times.add(now)

        // Anything older than the window is no longer interesting. The list is always in order
        // because entries are only ever appended, and the clock only moves forward, so dropping
        // from the front is enough.
        val cutoff = now - ValidationConstants.RECREATE_WINDOW_MS
        while (times.isNotEmpty() && times[0] < cutoff) {
            // removeAt(0) shifts the rest of the list along each time. The smarter way is an
            // ArrayDeque, which drops from the front without shifting; with a handful of entries it
            // makes no measurable difference here.
            times.removeAt(0)
        }

        if (times.size > ValidationConstants.RECREATE_LIMIT) {
            logger.report(
                activityName,
                "was destroyed and recreated ${times.size} times in the last " +
                    "${ValidationConstants.RECREATE_WINDOW_MS / 1000} seconds, not counting " +
                    "configuration changes — this looks like a restart loop",
            )
            // Cleared so the next report needs a fresh burst rather than firing again on every
            // single destroy for as long as the old timestamps stay inside the window.
            times.clear()
        }
    }
}
