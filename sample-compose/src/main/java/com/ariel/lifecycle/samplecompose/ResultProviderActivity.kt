package com.ariel.lifecycle.samplecompose

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ariel.lifecycle.samplecompose.ui.SampleTheme
import com.ariel.lifecycle.samplecompose.ui.ScreenScaffold

/** EXERCISE (no fault): the far end of the round trip. Returns a result and finishes. */
class ResultProviderActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SampleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    ScreenScaffold(
                        route = "ResultProviderActivity",
                        fault = "EXERCISE — the second Activity in the round trip; returns a result",
                    ) {
                        Text("Started by another screen.")
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Return a result, or just press Back — both paths are worth measuring.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(Modifier.height(20.dp))
                        Button(onClick = {
                            val data = Intent().putExtra(
                                EXTRA_PAYLOAD,
                                "returned at ${SystemClock.elapsedRealtime()}ms uptime",
                            )
                            setResult(Activity.RESULT_OK, data)
                            finish()
                        }) {
                            Text("Return RESULT_OK and finish")
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_PAYLOAD = "payload"
    }
}
