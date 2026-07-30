package com.ariel.lifecycle.samplecompose.screens

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ariel.lifecycle.samplecompose.MainActivity
import com.ariel.lifecycle.samplecompose.core.findActivity
import com.ariel.lifecycle.samplecompose.nav.Routes
import com.ariel.lifecycle.samplecompose.ui.ScreenScaffold
import kotlinx.coroutines.delay

private const val PASSES = 5
private const val DELAY_MS = 700L

/**
 * EXERCISE (no fault): finishes and relaunches the hosting Activity [PASSES] times.
 *
 * Each pass restarts MainActivity with this route in the Intent, so the app lands straight back
 * here — a burst of Activity destroy/create cycles without leaving Compose navigation.
 */
@Composable
fun RelaunchSelfScreen() {
    val activity = LocalContext.current.findActivity()
    val pass = activity.intent.getIntExtra(MainActivity.EXTRA_RELAUNCH_PASS, 1)

    ScreenScaffold(
        route = Routes.RELAUNCH_SELF,
        fault = "EXERCISE — finishes and relaunches the Activity $PASSES times",
    ) {
        Text("Pass $pass of $PASSES")
        Spacer(Modifier.height(12.dp))
        Text(
            "Activity @${Integer.toHexString(System.identityHashCode(activity))}",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(12.dp))

        if (pass < PASSES) {
            Text("Relaunching in ${DELAY_MS}ms…", style = MaterialTheme.typography.bodySmall)
            LaunchedEffect(pass) {
                delay(DELAY_MS)
                relaunch(activity, pass + 1)
            }
        } else {
            Text(
                "Done — $PASSES create/destroy cycles. Back returns to the route list.",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(20.dp))
            Button(onClick = { relaunch(activity, 1) }) {
                Text("Run the loop again")
            }
        }
    }
}

private fun relaunch(activity: Activity, pass: Int) {
    val next = Intent(activity, MainActivity::class.java)
        .putExtra(MainActivity.EXTRA_START_ROUTE, Routes.RELAUNCH_SELF)
        .putExtra(MainActivity.EXTRA_RELAUNCH_PASS, pass)
    activity.finish()
    activity.startActivity(next)
}
