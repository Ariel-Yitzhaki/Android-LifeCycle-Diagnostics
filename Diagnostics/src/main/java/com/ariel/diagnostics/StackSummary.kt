package com.ariel.diagnostics

import android.os.strictmode.Violation

/**
 * Turns a stack trace into one Logcat line.
 *
 * Shared by the features that have a stack to show: the main-thread watchdog, which samples the main
 * thread, and the two StrictMode watchers, which read the stack off the violation itself.
 */
internal object StackSummary {

    /**
     * Frames at the top of a StrictMode violation's stack that are always the detection machinery
     * rather than the code that caused the violation. Skipping them is what makes the difference
     * between a line naming `BlockGuardOs.read` and one naming the app class that touched the disk.
     */
    private val violationPlumbingPrefixes = arrayOf(
        "android.os.StrictMode",
        "libcore.io.BlockGuard",
        "libcore.io.ForwardingOs",
        "dalvik.system.BlockGuard",
    )

    /**
     * How many package segments an app package has to have before it is trusted. Two keeps a pair
     * that agree on nothing but "com" from matching every frame on every stack.
     */
    private const val MIN_PACKAGE_SEGMENTS = 2

    /**
     * Works out the package an app's own classes live under, which is what lets a finding point at
     * the app's line in a stack that is otherwise all framework and libraries.
     *
     * Neither input answers it alone. The installed id is not the code package whenever the build
     * adds an applicationIdSuffix, which debug builds commonly do and this library is meant to be
     * used from a debug build. The first screen's class name is one package too deep, since screens
     * usually sit in a subpackage of their own. What the two share is the root the app was written
     * under.
     *
     * Returns null when they share too little to be believed, in which case findings simply say
     * nothing about the app's own frames rather than guessing at them.
     */
    fun appPackageOf(applicationId: String, firstScreenClassName: String): String? {
        val idSegments = applicationId.split('.')
        val classSegments = firstScreenClassName.split('.')

        var shared = 0
        while (shared < idSegments.size &&
            shared < classSegments.size &&
            idSegments[shared] == classSegments[shared]
        ) {
            shared++
        }

        if (shared < MIN_PACKAGE_SEGMENTS) {
            return null
        }
        return idSegments.subList(0, shared).joinToString(".")
    }

    /**
     * Joins the first [limit] frames into one line, newest call first, and says how many were left
     * off the end.
     */
    fun describe(frames: Array<StackTraceElement>, limit: Int): String {
        if (frames.isEmpty()) {
            // getStackTrace() hands back an empty array for a thread that is not running.
            return "no stack available"
        }

        var shown = limit
        if (frames.size < shown) {
            shown = frames.size
        }

        val line = StringBuilder()
        for (index in 0 until shown) {
            if (index > 0) {
                line.append(" <- ")
            }
            line.append(frames[index].toString())
        }
        if (frames.size > shown) {
            line.append(" <- ")
            line.append(frames.size - shown)
            line.append(" more frames")
        }
        return line.toString()
    }

    /**
     * Describes where a StrictMode violation came from, which is the only thing most violations can
     * say about themselves: getMessage() is null for every thread violation and most VM ones.
     *
     * Two kinds of stack are involved. A thread violation, such as a disk read, is a bare Throwable
     * filled in where the offending call was made, so its own stack is what is wanted. A VM
     * violation is noticed long afterwards, on whichever thread ran the collector, and carries the
     * stack of the code that registered or allocated the leaked object as its cause. Preferring the
     * cause is therefore what turns a finalizer stack into the registration site.
     *
     * [appPackage] is what [appPackageOf] worked out, or null when it could not. A violation raised
     * deep inside a library can push the app's own frame past the ones printed, or leave the app off
     * the stack altogether, and both are worth saying: the first is the only line anyone can act on,
     * and the second means there is no such line at all.
     */
    fun describeViolation(violation: Violation, limit: Int, appPackage: String?): String {
        val origin: Throwable = violation.cause ?: violation
        val frames = dropPlumbing(origin.stackTrace)
        val stack = describe(frames, limit)

        if (appPackage == null) {
            // Nothing to match against, so the stack is all there is to say.
            return stack
        }

        val appFrameIndex = firstFrameIn(frames, appPackage)
        if (appFrameIndex < 0) {
            // Said out loud because it is the answer to "is this one mine". A violation with none of
            // the app's own code on the stack came from a library setting itself up, and there is no
            // line of the app's to go and change.
            return "$stack. No frame from $appPackage on this stack"
        }

        if (appFrameIndex < limit) {
            // Already printed above, near the top, which is the ordinary case.
            return stack
        }

        return "$stack. First frame from $appPackage: ${frames[appFrameIndex]}"
    }

    // Index of the first frame belonging to the given package, or -1. The dot is part of the test so
    // that a package named travel does not claim a frame from one named travelagency.
    private fun firstFrameIn(frames: Array<StackTraceElement>, appPackage: String): Int {
        for (index in frames.indices) {
            val className = frames[index].className
            if (className == appPackage || className.startsWith("$appPackage.")) {
                return index
            }
        }
        return -1
    }

    private fun dropPlumbing(frames: Array<StackTraceElement>): Array<StackTraceElement> {
        var first = 0
        while (first < frames.size && isPlumbing(frames[first])) {
            first++
        }

        // Every frame matched, which should not happen, but returning nothing would lose the only
        // evidence the finding has.
        if (first == 0 || first == frames.size) {
            return frames
        }

        return frames.copyOfRange(first, frames.size)
    }

    private fun isPlumbing(frame: StackTraceElement): Boolean {
        for (prefix in violationPlumbingPrefixes) {
            if (frame.className.startsWith(prefix)) {
                return true
            }
        }
        return false
    }
}
