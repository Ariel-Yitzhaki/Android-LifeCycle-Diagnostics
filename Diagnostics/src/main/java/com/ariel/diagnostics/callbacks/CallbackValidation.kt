package com.ariel.diagnostics.callbacks

import android.app.Application

/**
 * Entry point for callback validation: registers the feature's own Activity callbacks with the
 * framework.
 */
object CallbackValidation {

    // Guards against a second registration, which would report every finding twice.
    private var installed = false

    // Safe to call more than once; every call after the first does nothing.
    fun install(application: Application) {
        if (installed) {
            return
        }
        installed = true

        val logger = ValidationLogger()
        val recreateWatcher = RecreateWatcher(logger)
        val tracker = TransitionTracker(logger, recreateWatcher)
        val strictModeWatcher = StrictModeWatcher(logger)
        val fragmentCallbacks = FragmentValidationCallbacks(tracker)
        val activityCallbacks = ActivityValidationCallbacks(tracker, strictModeWatcher, fragmentCallbacks)

        application.registerActivityLifecycleCallbacks(activityCallbacks)
    }
}
