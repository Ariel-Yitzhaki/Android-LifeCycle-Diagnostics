package com.ariel.lifecycle.sampleviews.screens

import android.os.Bundle
import com.ariel.lifecycle.sampleviews.core.SampleFiles
import com.ariel.lifecycle.sampleviews.ui.SimpleScreenActivity

/** FAULT: reads a 4 MiB file from disk synchronously, on the main thread, in `onCreate`. */
class MainThreadDiskReadActivity : SimpleScreenActivity() {

    override val faultDescription =
        "FAULT — reads a 4 MiB file synchronously on the main thread in onCreate()"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Thousands of unbuffered read() syscalls plus a SHA-256, all on the UI thread.
        val receipt = SampleFiles.readBlocking(this)

        setStatus("Disk read completed on the main thread")
        setNote(receipt)
    }
}
