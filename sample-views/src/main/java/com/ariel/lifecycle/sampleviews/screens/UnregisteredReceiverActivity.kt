package com.ariel.lifecycle.sampleviews.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.ariel.lifecycle.sampleviews.ui.SimpleScreenActivity

/**
 * FAULT: registers a receiver against the Activity context in `onStart` and never unregisters it.
 *
 * Each start adds another registration; when the Activity is destroyed the framework logs
 * `android.app.IntentReceiverLeaked`, and the receiver keeps the Activity alive until then.
 */
class UnregisteredReceiverActivity : SimpleScreenActivity() {

    private var received = 0

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            received++
            render()
        }
    }

    override val faultDescription =
        "FAULT — registers a BroadcastReceiver in onStart() and never unregisters it"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        primaryButton("Send broadcast") {
            sendBroadcast(Intent(ACTION_PING).setPackage(packageName))
        }
        render()
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(
            this,
            receiver,
            IntentFilter(ACTION_PING),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        registrations++
        render()
    }

    // No onStop(). The registration outlives the Activity.

    private fun render() {
        setStatus("Broadcasts received by this instance: $received")
        setNote(
            "Registrations made since process start: $registrations\n" +
                "Unregistrations: 0 — rotate or leave, then check logcat for IntentReceiverLeaked."
        )
    }

    companion object {
        const val ACTION_PING = "com.ariel.lifecycle.sampleviews.PING"
        private var registrations = 0
    }
}
