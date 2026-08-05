package com.ariel.diagnostics.lifecycle

import android.util.Log
import java.util.Locale

/**
 * Decides whether a measurement is slow and prints it to Logcat as one human-readable line. The
 * only class in this feature that touches Log.
 */
class TimingLogger {

    // Prints one line for the timing: warn level when it is slow, debug otherwise.
    fun log(timing: LifecycleTiming) {
        val slow = isSlow(timing)
        val line = buildLine(timing, slow)
        if (slow) {
            Log.w(DiagnosticsConstants.LOG_TAG, line)
        } else {
            Log.d(DiagnosticsConstants.LOG_TAG, line)
        }
    }

    // Returns true when the timing is over the slow-callback threshold.
    private fun isSlow(timing: LifecycleTiming): Boolean {
        val thresholdNanos = DiagnosticsConstants.SLOW_CALLBACK_THRESHOLD_MS * DiagnosticsConstants.NANOS_PER_MILLI
        return timing.durationNanos > thresholdNanos
    }

    // Builds the printed line, for example:
    // SlowCreateActivity.onCreate took 412.35 ms  SLOW (over 50 ms)  [first time seen]
    private fun buildLine(timing: LifecycleTiming, slow: Boolean): String {
        val millis = timing.durationNanos.toDouble() / DiagnosticsConstants.NANOS_PER_MILLI
        // Locale.US so the decimal separator is always a dot and the lines stay greppable.
        val duration = String.format(Locale.US, "%.2f ms", millis)
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
