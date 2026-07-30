package com.ariel.diagnostics.lifecycle

import android.app.Application

/**
 * The way an app turns lifecycle timing on: call [install] once, from Application.onCreate.
 *
 * It builds the small pieces the feature is made of, hands them to each other, and registers the
 * Activity callbacks with the framework. After that everything is driven by the framework calling
 * those callbacks — there is nothing else to call and nothing to shut down.
 */
object LifecycleDiagnostics {

    // Stops a second install() from registering a second set of callbacks, which would print every
    // measurement twice. Only ever touched from Application.onCreate, which runs on the main
    // thread, so it needs no locking. Note an app that runs in several processes gets one of these
    // per process, which is correct: each process has its own Activities to measure.
    private var installed = false

    /** Starts measuring. Safe to call more than once; every call after the first does nothing. */
    fun install(application: Application) {
        if (installed) {
            return
        }
        installed = true

        // Built here rather than as singletons so the wiring is visible in one place: the seen-set
        // and the logger are shared by both callback classes, and the Activity callbacks own the
        // Fragment callbacks because they are the ones that attach them to each Activity.
        val seenScreens = SeenScreens()
        val logger = TimingLogger()
        val fragmentCallbacks = FragmentTimingCallbacks(seenScreens, logger)
        val activityCallbacks = ActivityTimingCallbacks(seenScreens, logger, fragmentCallbacks)

        // From here the framework calls activityCallbacks for every Activity in this process, and
        // activityCallbacks attaches fragmentCallbacks to each Activity that can host fragments.
        application.registerActivityLifecycleCallbacks(activityCallbacks)
    }
}
