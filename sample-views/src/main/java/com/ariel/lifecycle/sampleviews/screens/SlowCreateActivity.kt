package com.ariel.lifecycle.sampleviews.screens

import android.os.Bundle
import com.ariel.lifecycle.sampleviews.core.BusyWork
import com.ariel.lifecycle.sampleviews.ui.SimpleScreenActivity

/** FAULT: ~400 ms of real computation, inline on the main thread, in `onCreate`. */
class SlowCreateActivity : SimpleScreenActivity() {

    override val faultDescription =
        "FAULT — runs ${BLOCKING_MS}ms of real computation inline on the main thread in onCreate()"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Paid in full on every launch and every rotation, before the first frame can be drawn.
        val receipt = BusyWork.spinAndDescribe(BLOCKING_MS)

        setStatus("onCreate blocked the main thread for ~${BLOCKING_MS}ms")
        setNote(receipt)
    }

    companion object {
        const val BLOCKING_MS = 400L
    }
}
