package com.ariel.lifecycle.sampleviews.screens

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import com.ariel.lifecycle.sampleviews.core.SampleService
import com.ariel.lifecycle.sampleviews.ui.SimpleScreenActivity

/** CONTROL: the same bind, undone in `onStop`, so binds and unbinds balance. */
class ServiceBindLeakCleanActivity : SimpleScreenActivity() {

    private var bound = false
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

    override val faultDescription = "CONTROL — the same bind, unbound in onStop()"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }

    override fun onStart() {
        super.onStart()
        bindService(Intent(this, SampleService::class.java), connection, Context.BIND_AUTO_CREATE)
        bound = true
        binds++
        render()
    }

    override fun onStop() {
        if (bound) {
            unbindService(connection)
            bound = false
            connected = false
            unbinds++
        }
        super.onStop()
    }

    private fun render() {
        setStatus("Bound: $connected")
        setNote("Binds since process start: $binds — unbinds: $unbinds")
    }

    companion object {
        private var binds = 0
        private var unbinds = 0
    }
}
