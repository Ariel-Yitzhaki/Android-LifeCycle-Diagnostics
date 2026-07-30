package com.ariel.lifecycle.samplecompose.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.ariel.lifecycle.samplecompose.core.BusyWork
import com.ariel.lifecycle.samplecompose.nav.Routes
import com.ariel.lifecycle.samplecompose.ui.ScreenScaffold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val SLOW_RESUME_BLOCKING_MS = 400L

/**
 * FAULT: ~400 ms of real computation on every `ON_RESUME`, inline on the main thread.
 *
 * Inside a nav destination the lifecycle owner is the back stack entry, so this fires on every
 * return to the route as well as on every Activity resume.
 */
@Composable
fun SlowResumeScreen() {
    var resumes by remember { mutableIntStateOf(0) }
    var receipt by remember { mutableStateOf("waiting for the first resume…") }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        resumes++
        receipt = BusyWork.spinAndDescribe(SLOW_RESUME_BLOCKING_MS)
    }

    ScreenScaffold(
        route = Routes.SLOW_RESUME,
        fault = "FAULT — blocks the main thread for ${SLOW_RESUME_BLOCKING_MS}ms on every ON_RESUME",
    ) {
        Text("Resumes: $resumes — each one blocked the main thread for ~${SLOW_RESUME_BLOCKING_MS}ms")
        Spacer(Modifier.height(12.dp))
        Text(receipt, style = MaterialTheme.typography.bodySmall)
    }
}

/** CONTROL: identical per-resume work, dispatched off the main thread. */
@Composable
fun SlowResumeCleanScreen() {
    var resumes by remember { mutableIntStateOf(0) }
    var receipt by remember { mutableStateOf("waiting for the first resume…") }
    val scope = rememberCoroutineScope()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        resumes++
        receipt = "computing…"
        scope.launch {
            receipt = withContext(Dispatchers.Default) {
                BusyWork.spinAndDescribe(SLOW_RESUME_BLOCKING_MS)
            }
        }
    }

    ScreenScaffold(
        route = Routes.SLOW_RESUME_CLEAN,
        fault = "CONTROL — same ${SLOW_RESUME_BLOCKING_MS}ms per resume, on a background dispatcher",
    ) {
        Text("Resumes: $resumes — none of them blocked the main thread")
        Spacer(Modifier.height(12.dp))
        Text(receipt, style = MaterialTheme.typography.bodySmall)
    }
}
