package com.ariel.lifecycle.sampleviews.screens

import android.content.Intent
import android.os.Bundle
import com.ariel.lifecycle.sampleviews.core.BusyWork
import com.ariel.lifecycle.sampleviews.ui.SimpleScreenActivity

/** FAULT: ~400 ms of real computation, inline on the main thread, on *every* `onResume`. */
class SlowResumeActivity : SimpleScreenActivity() {

    private var resumeCount = 0

    override val faultDescription =
        "FAULT — runs ${BLOCKING_MS}ms of real computation inline on the main thread in onResume()"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        primaryButton("Leave and come back (re-triggers onResume)") {
            startActivity(Intent(this, ResultProviderActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        resumeCount++

        // Every return to this screen pays this again: back navigation, unlocking, rotation.
        val receipt = BusyWork.spinAndDescribe(BLOCKING_MS)

        setStatus("onResume #$resumeCount blocked the main thread for ~${BLOCKING_MS}ms")
        setNote(receipt)
    }

    companion object {
        const val BLOCKING_MS = 400L
    }
}
