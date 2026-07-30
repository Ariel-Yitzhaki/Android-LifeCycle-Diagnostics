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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ariel.lifecycle.samplecompose.core.SampleFiles
import com.ariel.lifecycle.samplecompose.nav.Routes
import com.ariel.lifecycle.samplecompose.ui.ScreenScaffold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** FAULT: reads a 4 MiB file from disk synchronously, on the main thread, during composition. */
@Composable
fun DiskReadScreen() {
    val context = LocalContext.current

    // Thousands of unbuffered read() syscalls plus a SHA-256, inside composition.
    val receipt = remember { SampleFiles.readBlocking(context) }

    ScreenScaffold(
        route = Routes.DISK_READ,
        fault = "FAULT — reads a 4 MiB file synchronously on the main thread during composition",
    ) {
        Text("Disk read completed on the main thread")
        Spacer(Modifier.height(12.dp))
        Text(receipt, style = MaterialTheme.typography.bodySmall)
    }
}

/** CONTROL: reads the same file, the same way, on `Dispatchers.IO`. */
@Composable
fun DiskReadCleanScreen() {
    val context = LocalContext.current
    val receipt by produceState<String?>(initialValue = null, context) {
        value = withContext(Dispatchers.IO) { SampleFiles.readBlocking(context) }
    }

    ScreenScaffold(
        route = Routes.DISK_READ_CLEAN,
        fault = "CONTROL — same 4 MiB read, moved to Dispatchers.IO",
    ) {
        Text("Disk read dispatched off the main thread")
        Spacer(Modifier.height(12.dp))
        Text(receipt ?: "reading…", style = MaterialTheme.typography.bodySmall)
    }
}
