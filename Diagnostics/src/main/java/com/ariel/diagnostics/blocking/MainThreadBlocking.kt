package com.ariel.diagnostics.blocking

import android.app.Application
import android.os.Looper

/**
 * Entry point for main-thread blocking detection: builds the pieces the feature is made of, starts
 * the one background thread it owns, attaches the printer that times main-thread messages, and
 * registers its own Activity callbacks with the framework.
 */
object MainThreadBlocking {

    // Guards against a second printer and a second registration, which would report everything
    // twice. Only touched from Application.onCreate on the main thread, so it needs no locking.
    private var installed = false

    // Starts watching the main thread. Safe to call more than once; every call after the first does
    // nothing.
    //
    // Belongs in debug builds only, more than the other three features: the printer attached below
    // is called twice for every message the main thread runs.
    fun install(application: Application) {
        if (installed) {
            return
        }
        installed = true

        val logger = BlockingLogger()
        val foregroundActivity = ForegroundActivityTracker()
        val watchdog = SlowMessageWatchdog(logger, foregroundActivity)
        val printer = MainThreadPrinter(watchdog)
        val strictModeThreadWatcher = StrictModeThreadWatcher(logger, foregroundActivity)
        val jankTracker = JankTracker(logger)
        val activityCallbacks =
            BlockingActivityCallbacks(strictModeThreadWatcher, jankTracker, foregroundActivity)

        // The background thread has to exist before the first message can be timed, so it is started
        // before the printer is attached below.
        watchdog.start()

        // A Looper holds one printer and there is no getter for it, so anything that calls
        // setMessageLogging after this line silently switches the slow-message detector off. The
        // startup line below is printed so the log says plainly who took the slot.
        Looper.getMainLooper().setMessageLogging(printer)

        logger.note(
            "this library has taken the main Looper's message logging slot to time main-thread " +
                "messages — anything that calls setMessageLogging after this switches that off",
        )

        application.registerActivityLifecycleCallbacks(activityCallbacks)
    }
}
