package com.ariel.lifecycle.sampleviews.core

import android.os.SystemClock
import java.util.Locale
import kotlin.math.sqrt

/**
 * Real CPU work with a predictable wall-clock cost.
 *
 * The loop is bounded by the clock rather than by an iteration count, so a screen advertised as
 * "400 ms" costs about 400 ms on a fast phone and on a slow emulator alike. Nothing here sleeps or
 * parks: the calling thread stays runnable and on-CPU for the whole duration, which is what makes
 * these screens reproducible fixtures rather than flaky ones.
 */
object BusyWork {

    /** Written to on every call so the JIT cannot prove the arithmetic below is dead. */
    @Volatile
    private var sink: Double = 0.0

    /** Iterations between clock checks. Small enough to land within a millisecond of the target. */
    private const val BATCH = 2048

    /** Spins for [durationMs] doing LCG + sqrt arithmetic. Returns the iteration count performed. */
    fun spin(durationMs: Long, seed: Long = 0x2545F4914F6CDD1DL): Long {
        val deadline = SystemClock.elapsedRealtime() + durationMs
        var state = seed or 1L
        var acc = 0.0
        var iterations = 0L
        while (SystemClock.elapsedRealtime() < deadline) {
            repeat(BATCH) {
                state = state * 6364136223846793005L + 1442695040888963407L
                acc += sqrt(((state ushr 11) and 0xFFFFFFFFL).toDouble())
            }
            iterations += BATCH
        }
        sink = acc
        return iterations
    }

    /** [spin], plus a human-readable receipt so a screen can prove the work really happened. */
    fun spinAndDescribe(durationMs: Long, seed: Long = 0x2545F4914F6CDD1DL): String {
        val started = SystemClock.elapsedRealtime()
        val iterations = spin(durationMs, seed)
        val elapsed = SystemClock.elapsedRealtime() - started
        return String.format(
            Locale.US,
            "%,d iterations in %d ms on %s (checksum %.3f)",
            iterations,
            elapsed,
            Thread.currentThread().name,
            sink,
        )
    }
}
