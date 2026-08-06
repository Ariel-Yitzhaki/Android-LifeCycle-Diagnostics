package com.ariel.lifecycle.sampleviews.screens

import android.os.Bundle
import com.ariel.lifecycle.sampleviews.core.SampleSocket
import com.ariel.lifecycle.sampleviews.ui.SimpleScreenActivity

/**
 * FAULT: opens a TCP connection on the main thread in `onCreate`.
 *
 * Android throws `NetworkOnMainThreadException` for most of this by itself, but only for the calls
 * it knows about, and only when the app targets a recent SDK. StrictMode's check is the one that
 * catches the rest: a socket opened by a native library, an old HTTP client, or a DNS lookup made
 * somewhere nobody thought to look.
 */
class MainThreadNetworkActivity : SimpleScreenActivity() {

    override val faultDescription =
        "FAULT — opens a TCP connection on the main thread in onCreate()"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val receipt = SampleSocket.connectBlocking()

        setStatus("Socket opened from the main thread")
        setNote("$receipt\n\nLoopback only — nothing leaves the device, and the check fires on the socket call itself.")
    }
}
