package com.ariel.diagnostics.lifecycle

import android.app.Application

/**
 * Entry point for lifecycle timing: registers the feature's Activity callbacks with the framework.
 */
object LifecycleDiagnostics {

    // Guards against a second registration, which would print every measurement twice.
    private var installed = false

    // Safe to call more than once; every call after the first does nothing.
    fun install(application: Application) {
        if (installed) {
            return
        }
        installed = true

        val seenScreens = SeenScreens()
        val logger = TimingLogger()
        val fragmentCallbacks = FragmentTimingCallbacks(seenScreens, logger)
        val activityCallbacks = ActivityTimingCallbacks(seenScreens, logger, fragmentCallbacks)

        application.registerActivityLifecycleCallbacks(activityCallbacks)
    }
}
