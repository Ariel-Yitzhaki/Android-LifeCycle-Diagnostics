package com.ariel.lifecycle.samplecompose

import android.app.Application
import android.os.Bundle
import android.os.Process
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ariel.lifecycle.samplecompose.core.GlobalListenerRegistry
import com.ariel.lifecycle.samplecompose.ui.SampleTheme
import com.ariel.lifecycle.samplecompose.ui.ScreenScaffold

/**
 * EXERCISE (no fault): the one Activity declared with `android:process=":secondary"`.
 *
 * It runs outside the nav graph by necessity — a different process cannot share MainActivity's
 * composition. Compare the pid here with the one on the home screen.
 */
class SecondaryProcessActivity : ComponentActivity() {

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
                        route = "SecondaryProcessActivity",
                        fault = "EXERCISE — runs in its own process (android:process=\":secondary\")",
                    ) {
                        Text("pid ${Process.myPid()} — process ${Application.getProcessName()}")
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "This process has its own copy of every singleton. " +
                                "GlobalListenerRegistry here holds ${GlobalListenerRegistry.size} " +
                                "entries, independent of the main process.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}
