package com.ariel.lifecycle.samplecompose.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ariel.lifecycle.samplecompose.ResultProviderActivity
import com.ariel.lifecycle.samplecompose.nav.Routes
import com.ariel.lifecycle.samplecompose.ui.ScreenScaffold

/**
 * EXERCISE (no fault): starts [ResultProviderActivity] and handles its result.
 *
 * Gives the diagnostics library a clean stop → start → resume round trip between two Activities.
 */
@Composable
fun StartForResultScreen() {
    val context = LocalContext.current
    var trips by rememberSaveable { mutableIntStateOf(0) }
    var lastResult by rememberSaveable { mutableStateOf("Nothing started yet.") }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        trips++
        val payload = result.data?.getStringExtra(ResultProviderActivity.EXTRA_PAYLOAD) ?: "none"
        val outcome = if (result.resultCode == Activity.RESULT_OK) "RESULT_OK" else "RESULT_CANCELED"
        lastResult = "Last result: $outcome — payload: $payload"
    }

    ScreenScaffold(
        route = Routes.START_FOR_RESULT,
        fault = "EXERCISE — starts another Activity and comes back, to drive back navigation",
    ) {
        Text("Round trips completed: $trips")
        Spacer(Modifier.height(12.dp))
        Text(lastResult, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(20.dp))
        Button(onClick = {
            launcher.launch(Intent(context, ResultProviderActivity::class.java))
        }) {
            Text("Start ResultProviderActivity")
        }
    }
}
