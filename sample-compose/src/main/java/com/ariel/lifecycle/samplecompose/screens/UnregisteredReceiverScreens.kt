package com.ariel.lifecycle.samplecompose.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ariel.lifecycle.samplecompose.nav.Routes
import com.ariel.lifecycle.samplecompose.ui.ScreenScaffold

const val ACTION_PING = "com.ariel.lifecycle.samplecompose.PING"

private object ReceiverTally {
    var registrations = 0
    var unregistrations = 0
}

/**
 * FAULT: registers a receiver against the Activity context on entry and never unregisters it.
 *
 * Each entry adds another registration; when the Activity is destroyed the framework logs
 * `android.app.IntentReceiverLeaked`, and until then the receiver keeps the Activity alive.
 */
@Composable
fun UnregisteredReceiverScreen() {
    val context = LocalContext.current
    var received by remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                received++
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(ACTION_PING),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        ReceiverTally.registrations++

        // The fix would be `context.unregisterReceiver(receiver)` here. Deliberately absent.
        onDispose { }
    }

    ScreenScaffold(
        route = Routes.UNREGISTERED_RECEIVER,
        fault = "FAULT — registers a BroadcastReceiver on entry and never unregisters it",
    ) {
        Text("Broadcasts received by this screen: $received")
        Spacer(Modifier.height(12.dp))
        Text(
            "Registrations since process start: ${ReceiverTally.registrations}\n" +
                "Unregistrations: ${ReceiverTally.unregistrations} — rotate or leave, then check " +
                "logcat for IntentReceiverLeaked.",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = {
            context.sendBroadcast(Intent(ACTION_PING).setPackage(context.packageName))
        }) {
            Text("Send broadcast")
        }
    }
}

/** CONTROL: same receiver, same filter, unregistered when the screen leaves composition. */
@Composable
fun UnregisteredReceiverCleanScreen() {
    val context = LocalContext.current
    var received by remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                received++
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(ACTION_PING),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        ReceiverTally.registrations++

        onDispose {
            context.unregisterReceiver(receiver)
            ReceiverTally.unregistrations++
        }
    }

    ScreenScaffold(
        route = Routes.UNREGISTERED_RECEIVER_CLEAN,
        fault = "CONTROL — same receiver, unregistered on dispose",
    ) {
        Text("Broadcasts received by this screen: $received")
        Spacer(Modifier.height(12.dp))
        Text(
            "Registrations since process start: ${ReceiverTally.registrations} — " +
                "unregistrations: ${ReceiverTally.unregistrations}",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = {
            context.sendBroadcast(Intent(ACTION_PING).setPackage(context.packageName))
        }) {
            Text("Send broadcast")
        }
    }
}
