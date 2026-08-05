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
     */
    fun describeViolation(violation: Violation, limit: Int): String {
        val origin: Throwable = violation.cause ?: violation
        return describe(dropPlumbing(origin.stackTrace), limit)
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
