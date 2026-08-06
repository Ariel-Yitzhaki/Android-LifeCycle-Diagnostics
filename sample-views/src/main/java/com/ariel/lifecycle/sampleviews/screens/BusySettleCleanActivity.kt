package com.ariel.lifecycle.sampleviews.screens

import android.os.Bundle
import android.os.SystemClock
import androidx.lifecycle.lifecycleScope
import com.ariel.lifecycle.sampleviews.core.BusyWork
import com.ariel.lifecycle.sampleviews.ui.SimpleScreenActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * CONTROL: the same chunks, the same total work, parsed off the main thread.
 *
 * The main thread does nothing here but draw each result, so the screen settles as fast as the
 * work allows instead of as fast as one thread allows.
 */
class BusySettleCleanActivity : SimpleScreenActivity() {

    private var work: Job? = null
    private var chunks = 0

    override val faultDescription =
        "CONTROL — the same chunks and the same total work, on Dispatchers.Default"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStatus("Chunks processed: 0")
        setNote("processing…")
    }

    override fun onResume() {
        super.onResume()
        val startedAtUptime = SystemClock.uptimeMillis()
        chunks = 0

        work = lifecycleScope.launch {
            while (SystemClock.uptimeMillis() - startedAtUptime <= TOTAL_MS) {
                withContext(Dispatchers.Default) { BusyWork.spin(CHUNK_MS) }
                chunks++
                val elapsed = SystemClock.uptimeMillis() - startedAtUptime
                setStatus("Chunks processed: $chunks (${chunks * CHUNK_MS}ms of work in ${elapsed}ms)")
            }
            setNote("Done. The main thread only ever drew the results.")
        }
    }

    override fun onPause() {
        super.onPause()
        work?.cancel()
        work = null
    }

    private companion object {
        const val CHUNK_MS = 60L
        const val TOTAL_MS = 6_000L
    }
}
