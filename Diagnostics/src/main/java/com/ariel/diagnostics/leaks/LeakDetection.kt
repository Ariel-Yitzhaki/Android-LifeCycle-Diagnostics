package com.ariel.diagnostics.leaks

import android.app.Application

/**
 * Entry point for leak detection: starts the one background thread the feature owns and registers
 * its own Activity callbacks with the framework.
 */
object LeakDetection {

    // Guards against a second registration and a second background thread, which would check and
    // report everything twice.
    private var installed = false

    // Safe to call more than once; every call after the first does nothing.
    fun install(application: Application) {
        if (installed) {
            return
        }
        installed = true

        val logger = LeakLogger()
        val tally = LeakTally(logger)
        val watcher = LeakWatcher(tally)
        val fragmentCallbacks = LeakFragmentCallbacks(watcher)
        val activityCallbacks = LeakActivityCallbacks(watcher, fragmentCallbacks)

        // The background thread has to exist before the first screen can be destroyed.
        watcher.start()

        application.registerActivityLifecycleCallbacks(activityCallbacks)
    }
}
