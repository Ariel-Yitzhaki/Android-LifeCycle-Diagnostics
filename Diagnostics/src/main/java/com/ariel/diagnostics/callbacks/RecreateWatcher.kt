package com.ariel.diagnostics.callbacks

import android.os.SystemClock

/**
 * Watches for one Activity class being destroyed and recreated repeatedly in a short time, which
 * points at a restart loop. Configuration changes are excluded, since they destroy and recreate an
 * Activity by design.
 */
class RecreateWatcher(private val logger: ValidationLogger) {

    // Keyed by class name and never by an Activity object: this map lives as long as the process,
    // so an Activity here would be the very leak the feature looks for.
    private val destroyTimes = HashMap<String, MutableList<Long>>()

    // Records that one instance of the class was destroyed, and reports thrash if too many have
    // been destroyed inside the recent window.
    fun onActivityDestroyed(activityName: String, configurationChange: Boolean) {
        if (configurationChange) {
            return
        }

        // elapsedRealtime() never jumps backwards, unlike currentTimeMillis(), which moves when the
        // clock is corrected.
        val now = SystemClock.elapsedRealtime()

        var times = destroyTimes[activityName]
        if (times == null) {
            times = ArrayList()
            destroyTimes[activityName] = times
        }
        times.add(now)

        // Entries are only ever appended and the clock only moves forward, so the list is in order
        // and dropping from the front is enough.
        val cutoff = now - ValidationConstants.RECREATE_WINDOW_MS
        while (times.isNotEmpty() && times[0] < cutoff) {
            times.removeAt(0)
        }

        if (times.size > ValidationConstants.RECREATE_LIMIT) {
            logger.report(
                activityName,
                "was destroyed and recreated ${times.size} times in the last " +
                    "${ValidationConstants.RECREATE_WINDOW_MS / 1000} seconds, not counting " +
                    "configuration changes. This looks like a restart loop",
            )
            // Cleared so the next report needs a fresh burst rather than firing on every destroy
            // while the old timestamps stay inside the window.
            times.clear()
        }
    }
}
