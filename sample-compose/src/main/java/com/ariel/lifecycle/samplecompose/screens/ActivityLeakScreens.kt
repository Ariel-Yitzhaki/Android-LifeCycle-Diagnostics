package com.ariel.lifecycle.samplecompose.screens

import android.annotation.SuppressLint
import android.app.Activity
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ariel.lifecycle.samplecompose.core.findActivity
import com.ariel.lifecycle.samplecompose.nav.Routes
import com.ariel.lifecycle.samplecompose.ui.ScreenScaffold

/**
 * FAULT: parks the hosting Activity in a singleton and never clears it.
 *
 * Rotate the device: the old MainActivity is destroyed but stays reachable from here, along with
 * everything it holds. The count only ever goes up.
 */
object LeakedActivities {

    @SuppressLint("StaticFieldLeak")
    var lastInstance: Activity? = null
        private set

    @SuppressLint("StaticFieldLeak")
    private val instances = mutableListOf<Activity>()

    val count: Int get() = instances.size

    fun retain(activity: Activity) {
        lastInstance = activity
        if (instances.none { it === activity }) instances += activity
    }
}

/** CONTROL: the same "current Activity" pointer, released when the screen goes away. */
object CurrentActivityHolder {

    @SuppressLint("StaticFieldLeak")
    private var current: Activity? = null

    val count: Int get() = if (current == null) 0 else 1

    fun set(activity: Activity) {
        current = activity
    }

    fun clear(activity: Activity) {
        if (current === activity) current = null
    }
}

@Composable
fun ActivityLeakScreen() {
    val activity = LocalContext.current.findActivity()
    val retainedCount = remember(activity) {
        LeakedActivities.retain(activity)
        LeakedActivities.count
    }

    ScreenScaffold(
        route = Routes.ACTIVITY_LEAK,
        fault = "FAULT — stores the Activity in a singleton field that is never cleared",
    ) {
        Text("Distinct Activity instances retained: $retainedCount")
        Spacer(Modifier.height(12.dp))
        Text(
            "This instance: @${Integer.toHexString(System.identityHashCode(activity))}\n" +
                "Rotate the device — the destroyed Activity stays reachable and the count grows.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
fun ActivityLeakCleanScreen() {
    val activity = LocalContext.current.findActivity()

    DisposableEffect(activity) {
        CurrentActivityHolder.set(activity)
        // The whole fix: give the reference up when this screen or Activity goes away.
        onDispose { CurrentActivityHolder.clear(activity) }
    }

    ScreenScaffold(
        route = Routes.ACTIVITY_LEAK_CLEAN,
        fault = "CONTROL — same singleton pointer, released on dispose",
    ) {
        Text("Activity instances retained: ${CurrentActivityHolder.count} (at most the live one)")
        Spacer(Modifier.height(12.dp))
        Text(
            "This instance: @${Integer.toHexString(System.identityHashCode(activity))}\n" +
                "Rotate or leave: the reference is dropped before the Activity is destroyed.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
