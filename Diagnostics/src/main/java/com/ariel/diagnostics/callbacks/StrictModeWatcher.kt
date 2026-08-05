package com.ariel.diagnostics.callbacks

import android.app.Activity
import android.os.StrictMode
import android.os.strictmode.Violation
import androidx.core.content.ContextCompat

/**
 * Switches on the three StrictMode VM checks that catch things left behind: leaked registration
 * objects, leaked closable objects and leaked Activities. Violations are received inside the app
 * through penaltyListener instead of only in the system log.
 */
class StrictModeWatcher(private val logger: ValidationLogger) : StrictMode.OnVmViolationListener {

    private var installed = false

    // A name and never an Activity object: this field lives as long as the process, so an Activity
    // here would leak exactly what detectActivityLeaks() below looks for.
    private var foregroundActivityName: String? = null

    // Builds the StrictMode policy and installs it, the first time it is called.
    //
    // Must run from onActivityPreCreated and not from the library's install(): apps often call
    // setVmPolicy() in Application.onCreate, and setVmPolicy replaces the whole policy, so a policy
    // installed any earlier would be thrown away.
    fun installIfNeeded(activity: Activity) {
        if (installed) {
            return
        }
        installed = true

        // Seeding the Builder with the policy in force keeps whatever the app switched on for
        // itself; a plain Builder() would silently switch off every check the app asked for.
        val existingPolicy = StrictMode.getVmPolicy()
        val builder = StrictMode.VmPolicy.Builder(existingPolicy)

        // Fires when a BroadcastReceiver, ServiceConnection or similar is garbage collected while
        // still registered.
        builder.detectLeakedRegistrationObjects()
        // Fires when a Cursor, file stream or similar is garbage collected while still open.
        builder.detectLeakedClosableObjects()
        // Fires when more instances of an Activity class are alive than there should be.
        builder.detectActivityLeaks()

        // A policy holds a single listener, so this replaces one the app may have set. The
        // application context is used so this long-lived policy cannot hold an Activity.
        val mainThreadExecutor = ContextCompat.getMainExecutor(activity.applicationContext)
        builder.penaltyListener(mainThreadExecutor, this)

        StrictMode.setVmPolicy(builder.build())

        logger.note(
            "StrictMode VM checks are on: leaked registration objects, leaked closable objects, " +
                "Activity leaks",
        )
    }

    fun onActivityResumed(activityName: String) {
        foregroundActivityName = activityName
    }

    // Called from onActivityPaused, which Android always runs before resuming the incoming
    // Activity, so this cannot wipe out a newer name.
    fun onActivityPaused() {
        foregroundActivityName = null
    }

    // Prints one StrictMode violation together with the Activity in the foreground at the time.
    // That name is a best guess: a leak is only noticed when the collector gets round to the
    // object, which can be long after the code that leaked it ran.
    override fun onVmViolation(violation: Violation) {
        val violationKind = violation.javaClass.simpleName

        // Read into a local so the value cannot change between the check and the use below.
        val foreground = foregroundActivityName
        val attribution = if (foreground == null) {
            "no Activity was in the foreground at the time"
        } else {
            "the Activity in the foreground at the time was $foreground"
        }

        val message = violation.message
        val details = if (message == null) {
            "no details"
        } else {
            // Newlines are swapped for spaces so one finding stays one greppable Logcat line.
            message.replace('\n', ' ')
        }

        logger.report(
            "StrictMode $violationKind:",
            "$details. Best guess at where this came from: $attribution (an Activity leak is " +
                "usually noticed long after the code that caused it ran, so this may be the wrong " +
                "screen)",
        )
    }
}
