package com.ariel.lifecycle.sampleviews.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.ariel.lifecycle.sampleviews.ui.SimpleScreenActivity

/** CONTROL: same receiver, same filter, unregistered in `onStop` so registrations stay balanced. */
class UnregisteredReceiverCleanActivity : SimpleScreenActivity() {

    private var received = 0
    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            received++
            render()
        }
    }

    override val faultDescription =
        "CONTROL — same receiver, unregistered in onStop()"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        primaryButton("Send broadcast") {
            sendBroadcast(Intent(UnregisteredReceiverActivity.ACTION_PING).setPackage(packageName))
        }
        render()
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(
            this,
            receiver,
            IntentFilter(UnregisteredReceiverActivity.ACTION_PING),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        registered = true
        registrations++
        render()
    }

    override fun onStop() {
        if (registered) {
            unregisterReceiver(receiver)
            registered = false
            unregistrations++
        }
        super.onStop()
    }

    private fun render() {
        setStatus("Broadcasts received by this instance: $received")
        setNote("Registrations since process start: $registrations — unregistrations: $unregistrations")
    }

    companion object {
        private var registrations = 0
        private var unregistrations = 0
    }
}
