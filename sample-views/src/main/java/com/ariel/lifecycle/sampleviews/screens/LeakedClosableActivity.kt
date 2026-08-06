package com.ariel.lifecycle.sampleviews.screens

import android.os.Bundle
import com.ariel.lifecycle.sampleviews.core.ClosableWork
import com.ariel.lifecycle.sampleviews.ui.SimpleScreenActivity
import kotlin.concurrent.thread

/**
 * FAULT: opens a file stream and abandons it without closing.
 *
 * Nothing goes wrong at the time. The complaint arrives later, from the finalizer, when the
 * collector reaches the abandoned stream and its CloseGuard notices it was never closed — which is
 * why this is a VM check and why the finding names the line that *opened* the stream rather than
 * anything happening now.
 *
 * A file descriptor left open is a real cost: they are a per-process resource, and a screen that
 * leaks one per visit eventually runs the process out of them.
 */
class LeakedClosableActivity : SimpleScreenActivity() {

    override val faultDescription =
        "FAULT — opens a FileInputStream and abandons it without closing"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStatus("Abandoning a stream…")
        setNote("")

        // Off the main thread on purpose: opening a file is also a disk read, and the disk-read
        // detector would answer a question this screen is not asking.
        thread(name = "leak-a-closable", isDaemon = true) {
            val receipt = ClosableWork.leakOneStream(this)
            // The finding needs a collection before the finalizer can complain. A real app would
            // never ask for one; the sample does so the lesson does not depend on waiting.
            ClosableWork.requestFinalization()
            runOnUiThread {
                setStatus("Stream abandoned")
                setNote("$receipt\n\nA collection has been requested, so the complaint should be along shortly.")
            }
        }
    }
}
