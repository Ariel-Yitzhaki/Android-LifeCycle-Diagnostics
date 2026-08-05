package com.ariel.diagnostics.lifecycle

import android.util.Log
import java.util.Locale

/** Decides whether a measurement is slow and prints it to Logcat as one line. */
class TimingLogger {

    // Warn level when the timing is slow, debug otherwise.
    fun log(timing: LifecycleTiming) {
        val slow = isSlow(timing)
        val line = buildLine(timing, slow)
        if (slow) {
            Log.w(DiagnosticsConstants.LOG_TAG, line)
        } else {
            Log.d(DiagnosticsConstants.LOG_TAG, line)
        }
    }

    private fun isSlow(timing: LifecycleTiming): Boolean {
        // Time on screen is however long the user chose to look at it, so there is no duration that
        // would count as too long.
        if (timing.kind == MeasurementKind.TIME_ON_SCREEN) {
            return false
        }

        val thresholdNanos = DiagnosticsConstants.SLOW_CALLBACK_THRESHOLD_MS * DiagnosticsConstants.NANOS_PER_MILLI
        return timing.durationNanos > thresholdNanos
    }

    // Builds the printed line, for example:
    // SlowCreateActivity.onCreate took 412.35 ms  SLOW (over 50 ms)  [first time seen]
    // HomeFeedFragment was on screen for 84797.41 ms
    // MapFragment took ~312.75 ms to build its view  SLOW (over 50 ms)
    private fun buildLine(timing: LifecycleTiming, slow: Boolean): String {
        val millis = timing.durationNanos.toDouble() / DiagnosticsConstants.NANOS_PER_MILLI
        // Locale.US so the decimal separator is always a dot.
        val duration = String.format(Locale.US, "%.2f ms", millis)

        val line = StringBuilder()
        line.append(timing.screenName)

        when (timing.kind) {
            MeasurementKind.TIME_ON_SCREEN -> {
                // Deliberately not worded as a callback duration: naming onPause here would suggest
                // the fragment spent this long inside it.
                line.append(" was on screen for ")
                line.append(duration)
            }

            MeasurementKind.VIEW_CREATION -> {
                // No callback name for the same reason: this one spans two of them.
                line.append(" took ~")
                line.append(duration)
                line.append(" to build its view")
            }

            MeasurementKind.EXACT, MeasurementKind.BETWEEN_CALLBACKS -> {
                line.append(".")
                line.append(timing.callbackName)
                line.append(" took ")
                if (timing.kind == MeasurementKind.BETWEEN_CALLBACKS) {
                    line.append("~")
                }
                line.append(duration)
            }
        }

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
        if (timing.kind == MeasurementKind.BETWEEN_CALLBACKS) {
            line.append("  [approx: gap since the previous callback, can include work from other components]")
        }
        if (timing.kind == MeasurementKind.VIEW_CREATION) {
            line.append("  [approx: covers onCreateView and onViewCreated, including any fragment inflated inside them]")
        }
        return line.toString()
    }
}
