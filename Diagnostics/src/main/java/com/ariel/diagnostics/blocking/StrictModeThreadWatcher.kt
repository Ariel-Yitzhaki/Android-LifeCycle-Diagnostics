package com.ariel.diagnostics.blocking

import android.app.Activity
import android.os.StrictMode
import android.os.strictmode.Violation
import androidx.core.content.ContextCompat
import com.ariel.diagnostics.StackSummary

/**
 * Switches on the three StrictMode thread checks that catch work which should never happen on the
 * main thread: disk reads, disk writes and network. Violations are received inside the app through
 * penaltyListener instead of only in the system log.
 */
class StrictModeThreadWatcher(
    private val logger: BlockingLogger,
    private val foregroundScreen: ForegroundScreenTracker,
) : StrictMode.OnThreadViolationListener {

    private var installed = false

    // Must run from onActivityPreCreated and not from the library's install(), for two reasons.
    // Apps often call setThreadPolicy() in Application.onCreate and that replaces the whole policy,
    // so anything installed earlier is thrown away. And a thread policy belongs to whichever thread
    // sets it, so running here leaves background threads alone.
    fun installIfNeeded(activity: Activity) {
        if (installed) {
            return
        }
        installed = true

        // Seeding the Builder with the policy in force keeps whatever the app switched on for
        // itself; a plain Builder() would silently switch off every check the app asked for.
        val existingPolicy = StrictMode.getThreadPolicy()
        val builder = StrictMode.ThreadPolicy.Builder(existingPolicy)

        // Fires on opening a file, touching SharedPreferences the first time, most SQLite queries.
        builder.detectDiskReads()
        builder.detectDiskWrites()
        // Android also blocks this outright on the main thread, but the check still catches the
        // cases the framework misses.
        builder.detectNetwork()

        // A policy holds a single listener, so this replaces one the app may have set. The
        // application context is used so this long-lived policy cannot hold a screen.
        val mainThreadExecutor = ContextCompat.getMainExecutor(activity.applicationContext)
        builder.penaltyListener(mainThreadExecutor, this)

        StrictMode.setThreadPolicy(builder.build())

        logger.note(
            "StrictMode main-thread checks are on: disk reads, disk writes, network",
        )
    }

    // Called by StrictMode on the main thread, shortly after the offending call was made.
    override fun onThreadViolation(violation: Violation) {
        val violationKind = violation.javaClass.simpleName

        // A Violation is a Throwable whose stack was filled in where the violation was detected, so
        // it names the exact call that touched the disk or the network. getMessage() is null for
        // every thread violation type, which is why the stack and not the message is printed here.
        val origin = StackSummary.describeViolation(
            violation,
            BlockingConstants.VIOLATION_FRAMES_LOGGED,
        )

        logger.report(
            "StrictMode $violationKind on the main thread ${foregroundScreen.describe()}, " +
                "caused by: $origin",
        )
    }
}
