package com.ariel.diagnostics.leaks

import android.app.Application

/**
 * The way an app turns leak detection on: call [install] once, from Application.onCreate.
 *
 * It builds the small pieces the feature is made of, hands them to each other, starts the one
 * background thread the feature owns, and registers its own Activity callbacks with the framework.
 * After that everything is driven by the framework calling those callbacks — there is nothing else
 * to call and nothing to shut down.
 *
 * This is a separate entry point from Feature 1's and Feature 2's and it registers its own set of
 * callbacks, so any of the three can be switched on without the others and none of them knows the
 * others exist.
 */
object LeakDetection {

    // Stops a second install() from registering a second set of callbacks and starting a second
    // background thread, which would check and report everything twice. Only ever touched from
    // Application.onCreate, which runs on the main thread, so it needs no locking. An app that runs
    // in several processes gets one of these per process, which is correct: each process has its
    // own screens and its own heap.
    private var installed = false

    /**
     * Starts watching for leaks. Safe to call more than once; every call after the first does
     * nothing.
     *
     * This one is worth keeping to debug builds. Feature 1 and Feature 2 only read what the
     * framework tells them, while this feature asks for a garbage collection a few seconds after
     * every screen the user leaves, and that is real work to be doing in a shipped app.
     */
    fun install(application: Application) {
        if (installed) {
            return
        }
        installed = true

        // Built here rather than as singletons so the wiring is visible in one place: the logger is
        // the only thing that prints, the tally is the only thing that decides what is worth
        // printing, the watcher is the only thing that feeds the tally, and the Activity callbacks
        // own the Fragment callbacks because they are the ones that attach them to each Activity.
        val logger = LeakLogger()
        val tally = LeakTally(logger)
        val watcher = LeakWatcher(tally)
        val fragmentCallbacks = LeakFragmentCallbacks(watcher)
        val activityCallbacks = LeakActivityCallbacks(watcher, fragmentCallbacks)

        // The background thread has to exist before the first screen can be destroyed, so it is
        // started here rather than on the first destroy. Starting it costs one idle thread in an
        // app that never destroys anything, which is not a case worth writing code for.
        watcher.start()

        // From here the framework calls activityCallbacks for every Activity in this process, and
        // activityCallbacks attaches fragmentCallbacks to each Activity that can host fragments.
        //
        // Registering a third set of callbacks alongside Feature 1's and Feature 2's is fine: the
        // framework keeps a list of them and calls each one in turn.
        application.registerActivityLifecycleCallbacks(activityCallbacks)
    }
}
