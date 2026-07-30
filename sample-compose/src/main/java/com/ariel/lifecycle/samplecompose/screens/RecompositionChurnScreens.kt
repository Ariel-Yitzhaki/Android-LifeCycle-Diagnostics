package com.ariel.lifecycle.samplecompose.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ariel.lifecycle.samplecompose.nav.Routes
import com.ariel.lifecycle.samplecompose.ui.ScreenScaffold
import com.ariel.lifecycle.samplecompose.ui.rememberCompositionCount
import kotlinx.coroutines.delay

private const val ROWS = 12
private const val TICK_MS = 16L

/** Unstable on purpose: a plain class with a public `var`, so Compose can never skip on it. */
class UnstableLabel(var text: String)

/** Stable: immutable, and marked so Compose treats it as skippable. */
@Stable
class StableLabel(val text: String)

/**
 * FAULT: recomposes the entire subtree ~60 times a second when almost none of it needs to.
 *
 * Three separate mistakes, all common:
 *  1. the ticker state is read at the top of the tree, so every child is invalidated;
 *  2. children take an unstable parameter type, so none of them can be skipped;
 *  3. those parameters are allocated fresh on each pass instead of being remembered.
 */
@Composable
fun RecompositionChurnScreen() {
    val ticks = remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(TICK_MS)
            ticks.intValue++
        }
    }

    // Mistake 1: the read happens here, so this whole composable re-runs on every tick.
    val tick = ticks.intValue

    ScreenScaffold(
        route = Routes.RECOMPOSITION_CHURN,
        fault = "FAULT — state read at the top of the tree, unstable params, nothing remembered",
    ) {
        Text("tick: $tick", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(12.dp))

        // Mistakes 2 and 3: a brand-new unstable instance on every single pass.
        ChurnHeader(UnstableLabel("header"))
        Spacer(Modifier.height(8.dp))
        repeat(ROWS) { index ->
            ChurnRow(index = index, label = UnstableLabel("row $index"))
        }
    }
}

@Composable
private fun ChurnHeader(label: UnstableLabel) {
    val count = rememberCompositionCount()
    Text("${label.text}: composed $count times")
}

@Composable
private fun ChurnRow(index: Int, label: UnstableLabel) {
    val count = rememberCompositionCount()
    Text("${label.text}: composed $count times", style = MaterialTheme.typography.bodySmall)
}

/**
 * CONTROL: the same 60 Hz ticker, driving exactly one leaf.
 *
 * The state read is deferred into the composable that displays it, the parameters are stable, and
 * the instances are remembered — so the header and the rows compose once and are never invalidated.
 */
@Composable
fun RecompositionChurnCleanScreen() {
    val ticks = remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(TICK_MS)
            ticks.intValue++
        }
    }

    val headerLabel = remember { StableLabel("header") }
    val rowLabels = remember { List(ROWS) { StableLabel("row $it") } }

    ScreenScaffold(
        route = Routes.RECOMPOSITION_CHURN_CLEAN,
        fault = "CONTROL — same ticker; state read deferred to the one leaf that needs it",
    ) {
        // The lambda defers the read: only CleanTicker subscribes to the ticker.
        CleanTicker(tick = { ticks.intValue })
        Spacer(Modifier.height(12.dp))

        CleanHeader(headerLabel)
        Spacer(Modifier.height(8.dp))
        rowLabels.forEachIndexed { index, label ->
            CleanRow(index = index, label = label)
        }
    }
}

@Composable
private fun CleanTicker(tick: () -> Int) {
    val count = rememberCompositionCount()
    Text("tick: ${tick()} — ticker composed $count times", style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun CleanHeader(label: StableLabel) {
    val count = rememberCompositionCount()
    Text("${label.text}: composed $count times")
}

@Composable
private fun CleanRow(index: Int, label: StableLabel) {
    val count = rememberCompositionCount()
    Text("${label.text}: composed $count times", style = MaterialTheme.typography.bodySmall)
}
