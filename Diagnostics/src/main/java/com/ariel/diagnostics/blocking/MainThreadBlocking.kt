package com.ariel.diagnostics.blocking

import android.app.Application
import android.os.Looper

/**
 * Entry point for main-thread blocking detection: starts the one background thread the feature
 * owns, attaches the printer that times main-thread messages, and registers its own Activity
 * callbacks with the framework.
 *
 * This feature is for debug builds only, because timing main-thread messages is not free. Setting a
 * Printer on a Looper does not merely route lines that were already being produced: it makes
 * Looper.loopOnce() build a description of every message it runs, including toString() on the
 * Handler and the callback, before the Printer is given a chance to ignore it. That cost is paid on
 * the main thread for every message, so the feature makes the thread it measures slightly slower
 * than it would otherwise be.
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
        val foregroundScreen = ForegroundScreenTracker()
        val busyTracker = MainThreadBusyTracker(logger, foregroundScreen)
        val watchdog = SlowMessageWatchdog(logger, foregroundScreen, busyTracker)
        val printer = MainThreadPrinter(watchdog)
        val strictModeThreadWatcher = StrictModeThreadWatcher(logger, foregroundScreen)
        val jankTracker = JankTracker(logger, foregroundScreen)
        val fragmentCallbacks = BlockingFragmentCallbacks(foregroundScreen, jankTracker, busyTracker)
        val activityCallbacks = BlockingActivityCallbacks(
            strictModeThreadWatcher,
            jankTracker,
            foregroundScreen,
            busyTracker,
            fragmentCallbacks,
        )

        // The background thread has to exist before the first message can be timed, so it is started
        // before the printer is attached below.
        watchdog.start()

        // A Looper holds one printer and there is no getter for it, so anything that calls
        // setMessageLogging after this line silently switches the slow-message detector off.
        Looper.getMainLooper().setMessageLogging(printer)

        logger.note(
            "this library has taken the main Looper's message logging slot to time main-thread " +
                "messages. Anything that calls setMessageLogging after this switches that off. " +
                "While it is set the Looper builds a description of every main-thread message, so " +
                "keep this feature out of release builds, and expect findings to overstate the " +
                "problem on an emulator or a debuggable build",
        )

        application.registerActivityLifecycleCallbacks(activityCallbacks)
    }
}
