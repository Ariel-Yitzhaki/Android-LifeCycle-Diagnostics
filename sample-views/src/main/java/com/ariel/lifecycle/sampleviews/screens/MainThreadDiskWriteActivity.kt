package com.ariel.lifecycle.sampleviews.screens

import android.os.Bundle
import com.ariel.lifecycle.sampleviews.core.SampleFiles
import com.ariel.lifecycle.sampleviews.ui.SimpleScreenActivity

/**
 * FAULT: writes 2 MiB and fsyncs it, synchronously, on the main thread in `onCreate`.
 *
 * A write is the half of disk I/O that is easy to talk yourself into: it feels like it can be
 * fired and forgotten. It cannot — the thread waits for the filesystem, and an fsync waits for the
 * hardware. Committing SharedPreferences, saving a draft and writing a cache entry are all this.
 */
class MainThreadDiskWriteActivity : SimpleScreenActivity() {

    override val faultDescription =
        "FAULT — writes and fsyncs 2 MiB synchronously on the main thread in onCreate()"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val receipt = SampleFiles.writeBlocking(this)

        setStatus("Disk write completed on the main thread")
        setNote(receipt)
    }
}
