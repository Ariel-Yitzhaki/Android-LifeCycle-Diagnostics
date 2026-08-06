package com.ariel.lifecycle.sampleviews.screens

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.ariel.lifecycle.sampleviews.core.BusyWork
import com.ariel.lifecycle.sampleviews.ui.SimpleScreenActivity

/**
 * FAULT: takes six seconds to settle, in messages small enough that none of them is a fault.
 *
 * This is the case no callback timing can show. Every lifecycle callback here returns in about a
 * millisecond, and no single main-thread message crosses the 200 ms line, so both of the other
 * detectors are right to stay quiet. The screen is still unusable for six seconds, because it
 * spends three quarters of every one of them working.
 *
 * A screen that loads in pieces looks exactly like this: a chunk of data arrives, is parsed and
 * laid out on the main thread, and the next chunk is already on its way.
 */
class BusySettleActivity : SimpleScreenActivity() {

    private val handler = Handler(Looper.getMainLooper())

    private var chunks = 0
    private var startedAtUptime = 0L

    override val faultDescription =
        "FAULT — ${CHUNK_MS}ms of main-thread work every ${CHUNK_MS + GAP_MS}ms, for ${TOTAL_MS / 1000} seconds"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }

    // Started from onResume, because that is when the screen counts as in front and the library
    // opens its window. Starting in onCreate would spend the first chunks before anyone is watching.
    override fun onResume() {
        super.onResume()
        startedAtUptime = SystemClock.uptimeMillis()
        chunks = 0
        handler.post(::processOneChunk)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacksAndMessages(null)
    }

    private fun processOneChunk() {
        val elapsed = SystemClock.uptimeMillis() - startedAtUptime
        if (elapsed > TOTAL_MS) {
            render()
            return
        }

        // Comfortably under the 200 ms line, so the slow-message detector has nothing to say.
        BusyWork.spin(CHUNK_MS)
        chunks++
        render()

        // A gap rather than a straight re-post, so the screen still answers a Back press.
        handler.postDelayed(::processOneChunk, GAP_MS)
    }

    private fun render() {
        val elapsed = SystemClock.uptimeMillis() - startedAtUptime
        setStatus("Chunks processed: $chunks (${chunks * CHUNK_MS}ms of work in ${elapsed}ms)")
        setNote(
            "No single message here is slow, and no lifecycle callback is either. The screen is " +
                "simply busy — which is all the user can actually feel."
        )
    }

    private companion object {
        const val CHUNK_MS = 60L
        const val GAP_MS = 20L
        const val TOTAL_MS = 6_000L
    }
}
