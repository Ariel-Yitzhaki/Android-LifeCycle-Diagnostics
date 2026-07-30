package com.ariel.lifecycle.sampleviews.screens

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.ariel.lifecycle.sampleviews.core.BusyWork
import com.ariel.lifecycle.sampleviews.ui.SimpleScreenActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** CONTROL: identical work, dispatched off the main thread. `onCreate` returns immediately. */
class SlowCreateCleanActivity : SimpleScreenActivity() {

    override val faultDescription =
        "CONTROL — same ${SlowCreateActivity.BLOCKING_MS}ms of work, moved to a background dispatcher"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStatus("onCreate returned without blocking")
        setNote("computing…")

        lifecycleScope.launch {
            val receipt = withContext(Dispatchers.Default) {
                BusyWork.spinAndDescribe(SlowCreateActivity.BLOCKING_MS)
            }
            setNote(receipt)
        }
    }
}
