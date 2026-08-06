package com.ariel.lifecycle.sampleviews.screens

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.ariel.lifecycle.sampleviews.core.SampleSocket
import com.ariel.lifecycle.sampleviews.ui.SimpleScreenActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** CONTROL: the same connection, on `Dispatchers.IO`. */
class MainThreadNetworkCleanActivity : SimpleScreenActivity() {

    override val faultDescription = "CONTROL — the same connection, moved to Dispatchers.IO"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStatus("Socket dispatched off the main thread")
        setNote("connecting…")

        lifecycleScope.launch {
            val receipt = withContext(Dispatchers.IO) { SampleSocket.connectBlocking() }
            setNote(receipt)
        }
    }
}
