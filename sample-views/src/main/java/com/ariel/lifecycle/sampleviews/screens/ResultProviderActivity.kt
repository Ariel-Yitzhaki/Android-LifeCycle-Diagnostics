package com.ariel.lifecycle.sampleviews.screens

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import com.ariel.lifecycle.sampleviews.ui.SimpleScreenActivity

/** EXERCISE (no fault): the far end of the round trip. Returns a result and finishes. */
class ResultProviderActivity : SimpleScreenActivity() {

    override val faultDescription = "EXERCISE — the second Activity in the round trip; returns a result"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStatus("Started by another screen.")
        setNote("Return a result, or just press Back — both paths are worth measuring.")

        primaryButton("Return RESULT_OK and finish") {
            val data = Intent().putExtra(EXTRA_PAYLOAD, "returned at ${SystemClock.elapsedRealtime()}ms uptime")
            setResult(Activity.RESULT_OK, data)
            finish()
        }
    }

    companion object {
        const val EXTRA_PAYLOAD = "payload"
    }
}
