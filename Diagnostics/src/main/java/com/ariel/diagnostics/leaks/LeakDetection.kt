package com.ariel.diagnostics.leaks

import android.app.Application

/**
 * Entry point for leak detection: builds the pieces the feature is made of, starts the one
 * background thread it owns, and registers its own Activity callbacks with the framework.
 */
object LeakDetection {

    // Guards against a second registration and a second background thread, which would check and
    // report everything twice. Only touched from Application.onCreate on the main thread.
    private var installed = false

    // Starts watching for leaks. Safe to call more than once; every call after the first does
    // nothing.
    //
    // Worth keeping to debug builds: this feature asks for a garbage collection a few seconds after
    // every screen the user leaves, which is real work to be doing in a shipped app.
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
