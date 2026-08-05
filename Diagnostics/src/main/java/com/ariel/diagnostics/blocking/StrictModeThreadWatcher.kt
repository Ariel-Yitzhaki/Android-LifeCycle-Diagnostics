package com.ariel.diagnostics.blocking

import android.app.Activity
import android.os.StrictMode
import android.os.strictmode.Violation
import androidx.core.content.ContextCompat

/**
 * Switches on the three StrictMode thread checks that catch work which should never happen on the
 * main thread — disk reads, disk writes and network — and receives the violations inside the app
 * through penaltyListener instead of only in the system log.
 *
 * Where SlowMessageWatchdog notices that the main thread is busy but cannot say why, this names the
 * exact kind of mistake at the moment it is made.
 *
 * TODO: merge with Feature 2's StrictModeWatcher, which is the same class with a VmPolicy.
 */
class StrictModeThreadWatcher(
    private val logger: BlockingLogger,
    private val foregroundActivity: ForegroundActivityTracker,
) : StrictMode.OnThreadViolationListener {

    private var installed = false

    // Builds the StrictMode thread policy and installs it, the first time it is called.
    //
    // Must run from onActivityPreCreated rather than from the library's install(), for two reasons.
    // Apps often call setThreadPolicy() in Application.onCreate, and setThreadPolicy replaces the
    // whole policy, so a policy installed any earlier would be thrown away by the app's own line.
    // And a thread policy belongs to whichever thread calls it: onActivityPreCreated runs on the
    // main thread, so background threads are correctly left alone.
    fun installIfNeeded(activity: Activity) {
        if (installed) {
            return
        }
        installed = true

        // Seeding the Builder with the policy in force copies across whatever the app switched on
        // for itself; a plain Builder() would silently switch off every check the app asked for.
        val existingPolicy = StrictMode.getThreadPolicy()
        val builder = StrictMode.ThreadPolicy.Builder(existingPolicy)

        // Fires on opening a file, touching SharedPreferences the first time, most SQLite queries.
        builder.detectDiskReads()
        builder.detectDiskWrites()
        // Android also blocks this outright on the main thread, but the check still catches the
        // cases the framework misses.
        builder.detectNetwork()

        // A policy holds a single listener, so this replaces one the app may have set. The main
        // thread is used because the foreground name a finding is labelled with is written from
        // lifecycle callbacks, and the application context is passed so this long-lived policy
        // cannot hold a screen.
        val mainThreadExecutor = ContextCompat.getMainExecutor(activity.applicationContext)
        builder.penaltyListener(mainThreadExecutor, this)

        StrictMode.setThreadPolicy(builder.build())

        logger.note(
            "StrictMode main-thread checks are on: disk reads, disk writes, network",
        )
    }

    // Prints one StrictMode violation together with the Activity in the foreground at the time.
    // Called by StrictMode on the main thread, shortly after the offending call was made.
    override fun onThreadViolation(violation: Violation) {
        val violationKind = violation.javaClass.simpleName

        val message = violation.message
        val details = if (message == null) {
            "no details"
        } else {
            // Newlines are swapped for spaces so one finding stays one greppable Logcat line.
            message.replace('\n', ' ')
        }

        logger.report(
            "StrictMode $violationKind on the main thread ${foregroundActivity.describe()}: $details",
        )
    }
}
