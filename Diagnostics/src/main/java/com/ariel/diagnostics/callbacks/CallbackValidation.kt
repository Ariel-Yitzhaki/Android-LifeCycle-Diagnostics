package com.ariel.diagnostics.callbacks

import android.app.Application

/**
 * The way an app turns callback validation on: call [install] once, from Application.onCreate.
 *
 * It builds the small pieces the feature is made of, hands them to each other, and registers its
 * own Activity callbacks with the framework. After that everything is driven by the framework
 * calling those callbacks — there is nothing else to call and nothing to shut down.
 *
 * This is a separate entry point from Feature 1's, and it registers a separate set of callbacks, so
 * either feature can be switched on without the other and neither knows about the other.
 */
object CallbackValidation {

    // Stops a second install() from registering a second set of callbacks, which would report every
    // finding twice. Only ever touched from Application.onCreate, which runs on the main thread, so
    // it needs no locking. An app that runs in several processes gets one of these per process,
    // which is correct: each process has its own Activities to validate.
    private var installed = false

    /** Starts validating. Safe to call more than once; every call after the first does nothing. */
    fun install(application: Application) {
        if (installed) {
            return
        }
        installed = true

        // Built here rather than as singletons so the wiring is visible in one place. The logger is
        // shared by everything that has something to report, the tracker owns the recreate watcher
        // because it is the only thing that feeds it, and the Activity callbacks own the Fragment
        // callbacks because they are the ones that attach them to each Activity.
        val logger = ValidationLogger()
        val recreateWatcher = RecreateWatcher(logger)
        val tracker = TransitionTracker(logger, recreateWatcher)
        val strictModeWatcher = StrictModeWatcher(logger)
        val fragmentCallbacks = FragmentValidationCallbacks(tracker)
        val activityCallbacks = ActivityValidationCallbacks(tracker, strictModeWatcher, fragmentCallbacks)

        // From here the framework calls activityCallbacks for every Activity in this process, and
        // activityCallbacks attaches fragmentCallbacks to each Activity that can host fragments and
        // installs the StrictMode policy when the first Activity is about to be created.
        //
        // Registering a second set of callbacks alongside Feature 1's is fine: the framework keeps
        // a list of them and calls each one in turn.
        application.registerActivityLifecycleCallbacks(activityCallbacks)
    }
}
