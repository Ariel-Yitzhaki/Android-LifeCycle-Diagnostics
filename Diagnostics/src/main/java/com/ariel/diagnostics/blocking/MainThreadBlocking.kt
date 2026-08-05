package com.ariel.diagnostics.blocking

import android.app.Application
import android.os.Looper

/**
 * Entry point for main-thread blocking detection: starts the one background thread the feature
 * owns, attaches the printer that times main-thread messages, and registers its own Activity
 * callbacks with the framework.
 */
object MainThreadBlocking {

    // Guards against a second printer and a second registration, which would report everything
    // twice.
    private var installed = false

    // Safe to call more than once; every call after the first does nothing.
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
        // setMessageLogging after this line silently switches the slow-message detector off.
        Looper.getMainLooper().setMessageLogging(printer)

        logger.note(
            "this library has taken the main Looper's message logging slot to time main-thread " +
                "messages. Anything that calls setMessageLogging after this switches that off",
        )

        application.registerActivityLifecycleCallbacks(activityCallbacks)
    }
}
