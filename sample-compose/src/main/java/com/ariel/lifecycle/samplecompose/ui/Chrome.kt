package com.ariel.lifecycle.samplecompose.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SampleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme(),
        content = content,
    )
}

/**
 * Shared chrome for every screen: the route name (so the fault is identifiable from the device) and
 * a one-line description of what it plants.
 *
 * Screens that host their own scrolling container pass `scrollable = false`.
 */
@Composable
fun ScreenScaffold(
    route: String,
    fault: String,
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val base = Modifier
        .fillMaxSize()
        .safeDrawingPadding()
        .padding(16.dp)
    val scrollState = rememberScrollState()

    Column(modifier = if (scrollable) base.verticalScroll(scrollState) else base) {
        Text(route, style = MaterialTheme.typography.titleLarge)
        Text(fault, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(20.dp))
        content()
    }
}

private class CompositionCounter {
    var value = 0
}

/**
 * Counts how many times the calling composable has been composed.
 *
 * Reading the returned Int does not itself subscribe to any state, so this observes recomposition
 * without causing any.
 */
@Composable
fun rememberCompositionCount(): Int {
    val counter = remember { CompositionCounter() }
    counter.value++
    return counter.value
}
