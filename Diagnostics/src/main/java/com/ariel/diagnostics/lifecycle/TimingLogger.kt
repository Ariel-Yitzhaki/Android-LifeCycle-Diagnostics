package com.ariel.diagnostics.lifecycle

import android.util.Log
import java.util.Locale

/**
 * Decides whether a measurement is slow and prints it to Logcat as one human-readable line.
 *
 * This is the only class in the feature that touches Log, so changing the output format — or
 * sending measurements somewhere other than Logcat later — means changing this file only.
 */
class TimingLogger {

    /** Prints one line for [timing]. */
    fun log(timing: LifecycleTiming) {
        val slow = isSlow(timing)
        val line = buildLine(timing, slow)
        // Slow callbacks go out at warn level so they stand out in Logcat and can be filtered on
        // their own; everything else stays at debug so the normal timings are there when I want
        // them without drowning the log.
        if (slow) {
            Log.w(DiagnosticsConstants.LOG_TAG, line)
        } else {
            Log.d(DiagnosticsConstants.LOG_TAG, line)
        }
    }

    private fun isSlow(timing: LifecycleTiming): Boolean {
        // Compared in nanoseconds rather than converting the duration to milliseconds first: whole
        // numbers, no rounding, and nothing sitting exactly on the threshold by accident.
        val thresholdNanos = DiagnosticsConstants.SLOW_CALLBACK_THRESHOLD_MS * DiagnosticsConstants.NANOS_PER_MILLI
        return timing.durationNanos > thresholdNanos
    }

    /**
     * Builds the printed line, for example:
     *
     * `SlowCreateActivity.onCreate took 412.35 ms  SLOW (over 50 ms)  [first time seen]`
     */
    private fun buildLine(timing: LifecycleTiming, slow: Boolean): String {
        val millis = timing.durationNanos.toDouble() / DiagnosticsConstants.NANOS_PER_MILLI
        // Locale.US so the decimal separator is always a dot. On a device set to, say, German the
        // default locale would print "412,35" and make the lines annoying to read and to grep.
        val duration = String.format(Locale.US, "%.2f ms", millis)
        // A leading "~" is the short signal that the number is a gap and not an exact measurement.
        // The line also spells that out at the end, so it reads correctly without knowing the "~".
        val approximateMark = if (timing.approximate) "~" else ""

        val line = StringBuilder()
        line.append(timing.screenName)
        line.append(".")
        line.append(timing.callbackName)
        line.append(" took ")
        line.append(approximateMark)
        line.append(duration)
        if (slow) {
            line.append("  SLOW (over ")
            line.append(DiagnosticsConstants.SLOW_CALLBACK_THRESHOLD_MS)
            line.append(" ms)")
        }
        if (timing.firstSeen) {
            line.append("  [first time seen]")
        }
        if (timing.configurationChange) {
            line.append("  [configuration change]")
        }
        if (timing.approximate) {
            line.append("  [approx: measured between callbacks, not around one]")
        }
        return line.toString()
    }
}
