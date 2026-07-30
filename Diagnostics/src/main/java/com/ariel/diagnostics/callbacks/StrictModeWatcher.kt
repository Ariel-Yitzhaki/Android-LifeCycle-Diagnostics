package com.ariel.diagnostics.callbacks

import android.app.Activity
import android.os.StrictMode
import android.os.strictmode.Violation
import androidx.core.content.ContextCompat

/**
 * Check B. Switches on the three StrictMode VM checks that catch things being left behind — leaked
 * registration objects, leaked closable objects and leaked Activities — and receives the reports
 * inside the app instead of only in the system log.
 *
 * StrictMode is a debugging mode built into Android. Detections are switched on with a policy; each
 * detection watches for one kind of mistake, and the penalties on the policy decide what happens
 * when one is found. The penalty used here is penaltyListener, which simply hands the violation
 * back to this class.
 *
 * This class also keeps the name of the Activity currently in the foreground so a violation can be
 * printed next to it. [onVmViolation] explains why that is a guess and not a fact.
 */
class StrictModeWatcher(private val logger: ValidationLogger) : StrictMode.OnVmViolationListener {

    // Stops the second and every later Activity from installing the policy again. Only ever touched
    // from onActivityPreCreated, which runs on the main thread, so it needs no locking.
    private var installed = false

    // The simple class name of the Activity that is in the foreground right now, or null when none
    // is. A name and never an Activity object: this field lives as long as the process, and holding
    // an Activity here would leak exactly what detectActivityLeaks() below is looking for.
    private var foregroundActivityName: String? = null

    /**
     * Builds the StrictMode policy and installs it, the first time it is called.
     *
     * Called by [ActivityValidationCallbacks] from onActivityPreCreated — the moment just before
     * the very first Activity in the process runs its own onCreate. Every later call does nothing.
     *
     * It has to happen here and not from the library's install() function, which runs inside the
     * app's Application.onCreate. Apps very often call StrictMode.setVmPolicy() themselves in
     * Application.onCreate, and setVmPolicy replaces the whole policy rather than adding to it, so
     * a policy installed from there would be thrown away moments later by the app's own line. By
     * the time the first Activity is about to be created, Application.onCreate has finished and
     * ours is the last word.
     */
    fun installIfNeeded(activity: Activity) {
        if (installed) {
            return
        }
        installed = true

        // getVmPolicy() returns the policy in force right now, including whatever the app switched
        // on for itself. Handing it to the Builder copies all of that across, so the three
        // detections below are added to the app's settings instead of wiping them out. Starting
        // from a plain Builder() would silently switch off every check the app had asked for.
        val existingPolicy = StrictMode.getVmPolicy()
        val builder = StrictMode.VmPolicy.Builder(existingPolicy)

        // A registration object is something registered with the system that must be unregistered
        // again: a BroadcastReceiver, a ServiceConnection. This fires when one is garbage collected
        // while still registered.
        builder.detectLeakedRegistrationObjects()
        // A closable object is something holding an operating system handle that must be closed: a
        // Cursor, a file stream. This fires when one is garbage collected while still open.
        builder.detectLeakedClosableObjects()
        // This fires when more instances of an Activity class are alive than there should be, which
        // is what happens when a destroyed Activity is still referenced by something long-lived.
        builder.detectActivityLeaks()

        // penaltyListener is what makes the violations visible inside the app: instead of only
        // being written to the system log, each one is handed to the listener — this class, which
        // implements StrictMode.OnVmViolationListener at the top of the file.
        //
        // This is the one part of the policy that cannot be added to. A policy holds a single
        // listener, so if the app had already set one of its own, ours takes its place.
        //
        // The Executor decides which thread the listener runs on. The main thread is used because
        // the foreground Activity name below is written from lifecycle callbacks on the main
        // thread, and reading it from another thread would need extra care to see the latest value.
        // The application context is passed rather than the Activity so that this long-lived policy
        // cannot end up holding an Activity.
        val mainThreadExecutor = ContextCompat.getMainExecutor(activity.applicationContext)
        builder.penaltyListener(mainThreadExecutor, this)

        // build() turns the collected settings into a policy and setVmPolicy() makes it the one in
        // force for this process.
        StrictMode.setVmPolicy(builder.build())

        logger.note(
            "StrictMode VM checks are on: leaked registration objects, leaked closable objects, " +
                "Activity leaks",
        )
    }

    /**
     * Remembers which Activity is in the foreground.
     *
     * Called by [ActivityValidationCallbacks] from onActivityResumed, every time any Activity comes
     * to the foreground.
     */
    fun onActivityResumed(activityName: String) {
        foregroundActivityName = activityName
    }

    /**
     * Forgets the foreground Activity, because it is leaving the foreground and nothing has taken
     * its place yet.
     *
     * Called by [ActivityValidationCallbacks] from onActivityPaused. Android always pauses the
     * outgoing Activity before it resumes the incoming one, so clearing here and setting again on
     * the next resume cannot wipe out a newer name. It does mean the name is null for the short
     * moment between two screens, and while the app is in the background.
     *
     * If findings too often print with no Activity name, the alternative is to keep the last
     * resumed name instead of clearing it and print it as "last seen" rather than "foreground".
     */
    fun onActivityPaused() {
        foregroundActivityName = null
    }

    /**
     * Prints one StrictMode violation, together with the Activity that was in the foreground when
     * it arrived.
     *
     * Called by StrictMode itself, on the main thread, some time after a violation is detected.
     *
     * The Activity name is a best guess at who is responsible and can easily be wrong. A leak is
     * only noticed when the garbage collector gets round to the object, which can be long after the
     * code that leaked it ran, and by then the user may well be on a different screen — or on none
     * at all. Read the name as "this is where the app was when the problem surfaced", not as "this
     * screen caused it".
     */
    override fun onVmViolation(violation: Violation) {
        // The kind of violation is carried by the class of the object itself, for example
        // LeakedClosableViolation or InstanceCountViolation, which is what an Activity leak is
        // reported as. Violation extends Throwable, so it also carries a message and a stack trace;
        // only the message is printed here to keep each finding to one line.
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
            // Some violation messages run to several lines. Newlines are swapped for spaces so one
            // finding stays one line in Logcat and can be grepped like the rest.
            message.replace('\n', ' ')
        }

        logger.report(
            "StrictMode $violationKind:",
            "$details — best guess at where this came from: $attribution (an Activity leak is " +
                "usually noticed long after the code that caused it ran, so this may be the wrong " +
                "screen)",
        )
    }
}
