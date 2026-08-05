package com.ariel.diagnostics.callbacks

import android.app.Application

/**
 * Entry point for callback validation: builds the pieces the feature is made of and registers its
 * own Activity callbacks with the framework, separately from the other features'.
 */
object CallbackValidation {

    // Guards against a second registration, which would report every finding twice. Only touched
    // from Application.onCreate on the main thread, so it needs no locking.
    private var installed = false

    // Starts validating. Safe to call more than once; every call after the first does nothing.
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
