package com.ariel.lifecycle.sampleviews.screens

import android.os.Bundle
import com.ariel.lifecycle.sampleviews.core.ClosableWork
import com.ariel.lifecycle.sampleviews.ui.SimpleScreenActivity
import kotlin.concurrent.thread

/** CONTROL: the same open and the same read, wrapped in `use {}` so it closes either way. */
class LeakedClosableCleanActivity : SimpleScreenActivity() {

    override val faultDescription = "CONTROL — the same stream, closed by use {}"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStatus("Reading and closing…")
        setNote("")

        thread(name = "close-a-closable", isDaemon = true) {
            val receipt = ClosableWork.readOneStream(this)
            // The same collection the FAULT screen asks for, so the two are judged the same way.
            ClosableWork.requestFinalization()
            runOnUiThread {
                setStatus("Stream closed")
                setNote("$receipt\n\nA collection has been requested too, and there is nothing to complain about.")
            }
        }
    }
}
