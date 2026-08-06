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

        // Printed so the tag exists from launch. Every other line this feature prints is a problem
        // it found, so without this one a healthy app would never print under the tag at all, and
        // there would be no way to tell "nothing was retained" from "the library never started".
        logger.note(
            "watching every destroyed Activity, Fragment and Fragment view. Nothing more is " +
                "printed here until one class has been destroyed at least " +
                "${LeakConstants.MIN_DESTROY_COUNT} times with half of those still in memory, so " +
                "silence under this tag is the good answer",
        )

        application.registerActivityLifecycleCallbacks(activityCallbacks)
    }
}
