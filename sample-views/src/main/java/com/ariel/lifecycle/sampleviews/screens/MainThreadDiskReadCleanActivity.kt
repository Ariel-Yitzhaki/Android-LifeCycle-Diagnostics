package com.ariel.lifecycle.sampleviews.screens

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.ariel.lifecycle.sampleviews.core.SampleFiles
import com.ariel.lifecycle.sampleviews.ui.SimpleScreenActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** CONTROL: reads the same file, the same way, on `Dispatchers.IO`. */
class MainThreadDiskReadCleanActivity : SimpleScreenActivity() {

    override val faultDescription = "CONTROL — same 4 MiB read, moved to Dispatchers.IO"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStatus("Disk read dispatched off the main thread")
        setNote("reading…")

        lifecycleScope.launch {
            val receipt = withContext(Dispatchers.IO) {
                SampleFiles.readBlocking(this@MainThreadDiskReadCleanActivity)
            }
            setNote(receipt)
        }
    }
}
