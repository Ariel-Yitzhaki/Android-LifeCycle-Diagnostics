package com.ariel.lifecycle.samplecompose.screens

import android.app.Application
import android.content.Intent
import android.os.Process
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ariel.lifecycle.samplecompose.SecondaryProcessActivity
import com.ariel.lifecycle.samplecompose.core.GlobalListenerRegistry
import com.ariel.lifecycle.samplecompose.nav.Routes
import com.ariel.lifecycle.samplecompose.ui.ScreenScaffold

/**
 * EXERCISE (no fault): launches the one Activity declared with `android:process=":secondary"`.
 *
 * That Activity gets its own Application instance, its own singletons and its own main looper.
 */
@Composable
fun SecondaryProcessScreen() {
    val context = LocalContext.current

    ScreenScaffold(
        route = Routes.SECONDARY_PROCESS,
        fault = "EXERCISE — launches an Activity declared with android:process=\":secondary\"",
    ) {
        Text("This process: pid ${Process.myPid()} (${Application.getProcessName()})")
        Spacer(Modifier.height(12.dp))
        Text(
            "GlobalListenerRegistry here holds ${GlobalListenerRegistry.size} entries. The other " +
                "process keeps its own, unrelated copy.",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = {
            context.startActivity(Intent(context, SecondaryProcessActivity::class.java))
        }) {
            Text("Launch SecondaryProcessActivity")
        }
    }
}
