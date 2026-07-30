package com.ariel.lifecycle.samplecompose.screens

import android.app.Application
import android.os.Process
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ariel.lifecycle.samplecompose.nav.RouteCatalog
import com.ariel.lifecycle.samplecompose.nav.Routes
import com.ariel.lifecycle.samplecompose.ui.ScreenScaffold

/**
 * Lists every route so any of them can be reached, backed out of, and rotated.
 *
 * Plants nothing itself: the diagnostics library should have nothing to say about this screen.
 */
@Composable
fun HomeScreen(onOpen: (String) -> Unit) {
    ScreenScaffold(
        route = Routes.HOME,
        fault = "No fault — lists every route in the app",
        scrollable = false,
    ) {
        Text(
            "sample-compose · ${RouteCatalog.entries.size} routes · " +
                "pid ${Process.myPid()} (${Application.getProcessName()})",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(12.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(RouteCatalog.entries, key = { it.route }) { entry ->
                Button(
                    onClick = { onOpen(entry.route) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(entry.route)
                }
                Text(
                    entry.fault,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 12.dp),
                )
            }
        }
    }
}
