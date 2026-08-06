package com.ariel.lifecycle.sampleviews.screens

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.ariel.lifecycle.sampleviews.core.SampleFiles
import com.ariel.lifecycle.sampleviews.ui.SimpleScreenActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** CONTROL: the same write and the same fsync, on `Dispatchers.IO`. */
class MainThreadDiskWriteCleanActivity : SimpleScreenActivity() {

    override val faultDescription = "CONTROL — the same 2 MiB write, moved to Dispatchers.IO"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStatus("Disk write dispatched off the main thread")
        setNote("writing…")

        lifecycleScope.launch {
            val receipt = withContext(Dispatchers.IO) {
                SampleFiles.writeBlocking(this@MainThreadDiskWriteCleanActivity)
            }
            setNote(receipt)
        }
    }
}
