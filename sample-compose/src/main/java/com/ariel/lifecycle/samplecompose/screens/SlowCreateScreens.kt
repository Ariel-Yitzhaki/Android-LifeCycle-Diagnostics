package com.ariel.lifecycle.samplecompose.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ariel.lifecycle.samplecompose.core.BusyWork
import com.ariel.lifecycle.samplecompose.nav.Routes
import com.ariel.lifecycle.samplecompose.ui.ScreenScaffold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

const val SLOW_CREATE_BLOCKING_MS = 400L

/** FAULT: ~400 ms of real computation runs inside the first composition, on the main thread. */
@Composable
fun SlowCreateScreen() {
    // `remember` runs this exactly once per entry to the route — inline, on the composition thread.
    // No frame can be produced until it returns.
    val receipt = remember { BusyWork.spinAndDescribe(SLOW_CREATE_BLOCKING_MS) }

    ScreenScaffold(
        route = Routes.SLOW_CREATE,
        fault = "FAULT — blocks the main thread for ${SLOW_CREATE_BLOCKING_MS}ms during first composition",
    ) {
        Text("First composition blocked the main thread for ~${SLOW_CREATE_BLOCKING_MS}ms")
        Spacer(Modifier.height(12.dp))
        Text(receipt, style = MaterialTheme.typography.bodySmall)
    }
}

/** CONTROL: identical work, produced off the main thread; composition is never blocked. */
@Composable
fun SlowCreateCleanScreen() {
    val receipt by produceState<String?>(initialValue = null) {
        value = withContext(Dispatchers.Default) {
            BusyWork.spinAndDescribe(SLOW_CREATE_BLOCKING_MS)
        }
    }

    ScreenScaffold(
        route = Routes.SLOW_CREATE_CLEAN,
        fault = "CONTROL — same ${SLOW_CREATE_BLOCKING_MS}ms of work, on a background dispatcher",
    ) {
        Text("First composition returned without blocking")
        Spacer(Modifier.height(12.dp))
        Text(receipt ?: "computing…", style = MaterialTheme.typography.bodySmall)
    }
}
