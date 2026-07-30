package com.ariel.lifecycle.sampleviews.screens

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.ariel.lifecycle.sampleviews.core.BusyWork
import com.ariel.lifecycle.sampleviews.ui.SimpleScreenActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** CONTROL: identical per-resume work, dispatched off the main thread and cancelled on pause. */
class SlowResumeCleanActivity : SimpleScreenActivity() {

    private var resumeCount = 0
    private var work: Job? = null

    override val faultDescription =
        "CONTROL — same ${SlowResumeActivity.BLOCKING_MS}ms per resume, on a background dispatcher"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        primaryButton("Leave and come back (re-triggers onResume)") {
            startActivity(Intent(this, ResultProviderActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        resumeCount++

        setStatus("onResume #$resumeCount returned without blocking")
        setNote("computing…")

        work = lifecycleScope.launch {
            val receipt = withContext(Dispatchers.Default) {
                BusyWork.spinAndDescribe(SlowResumeActivity.BLOCKING_MS)
            }
            setNote(receipt)
        }
    }

    override fun onPause() {
        super.onPause()
        work?.cancel()
        work = null
    }
}
