package com.ariel.lifecycle.sampleviews.screens

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import com.ariel.lifecycle.sampleviews.core.SampleService
import com.ariel.lifecycle.sampleviews.ui.SimpleScreenActivity

/**
 * FAULT: binds to a service in `onStart` and never unbinds.
 *
 * A `ServiceConnection` is a registration object, like a receiver, and the framework keeps it
 * against the Context that registered it. When that Context is an Activity being destroyed with
 * the binding still outstanding, the framework raises the complaint itself and StrictMode's VM
 * check turns it into a finding.
 *
 * The connection is an anonymous inner class here — as it nearly always is — so it also holds the
 * Activity, which is the second thing that goes wrong.
 */
class ServiceBindLeakActivity : SimpleScreenActivity() {

    private var connected = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            connected = true
            render()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            connected = false
            render()
        }
    }

    override val faultDescription =
        "FAULT — binds to a Service in onStart() and never unbinds"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }

    override fun onStart() {
        super.onStart()
        bindService(Intent(this, SampleService::class.java), connection, Context.BIND_AUTO_CREATE)
        binds++
        render()
    }

    // No onStop(). The binding outlives the Activity that made it.

    private fun render() {
        setStatus("Bound: $connected")
        setNote(
            "Binds made since process start: $binds\nUnbinds: 0 — press Back and the framework " +
                "notices the connection this screen left behind."
        )
    }

    companion object {
        private var binds = 0
    }
}
