package com.ariel.lifecycle.samplecompose.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ariel.lifecycle.samplecompose.core.HeavyRows
import com.ariel.lifecycle.samplecompose.nav.Routes
import com.ariel.lifecycle.samplecompose.ui.ScreenScaffold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * FAULT: every item composition burns [HeavyRows.COST_MS] on the main thread, with no cache.
 *
 * A fling composes rows faster than 12 ms apiece allows, so frames are dropped for as long as the
 * list is moving — every time, in both directions.
 */
@Composable
fun JankListScreen() {
    ScreenScaffold(
        route = Routes.JANK_LIST,
        fault = "FAULT — ${HeavyRows.COST_MS}ms of blocking work per row, inside item composition",
        scrollable = false,
    ) {
        Text(
            "${HeavyRows.ROW_COUNT} rows, recomputed on every composition. Scroll to drop frames.",
            style = MaterialTheme.typography.bodySmall,
        )
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(HeavyRows.ROW_COUNT) { index ->
                // Blocking, on the main thread, every time this row scrolls into view.
                Text(
                    HeavyRows.compute(index),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                )
            }
        }
    }
}

/** CONTROL: identical rows and identical per-row cost, computed off the main thread and memoised. */
@Composable
fun JankListCleanScreen() {
    ScreenScaffold(
        route = Routes.JANK_LIST_CLEAN,
        fault = "CONTROL — same ${HeavyRows.COST_MS}ms per row, computed on Dispatchers.Default and cached",
        scrollable = false,
    ) {
        Text(
            "${HeavyRows.ROW_COUNT} rows. Rows fill in as they resolve; scrolling never blocks.",
            style = MaterialTheme.typography.bodySmall,
        )
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(HeavyRows.ROW_COUNT) { index ->
                val text by produceState(
                    initialValue = HeavyRows.cached(index) ?: "Row $index — computing…",
                    key1 = index,
                ) {
                    HeavyRows.cached(index)?.let { value = it; return@produceState }
                    value = withContext(Dispatchers.Default) { HeavyRows.computeAndCache(index) }
                }
                Text(
                    text,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                )
            }
        }
    }
}
