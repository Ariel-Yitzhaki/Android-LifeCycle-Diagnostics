package com.ariel.lifecycle.samplecompose.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ariel.lifecycle.samplecompose.core.GlobalListenerRegistry
import com.ariel.lifecycle.samplecompose.nav.Routes
import com.ariel.lifecycle.samplecompose.ui.ScreenScaffold

/**
 * FAULT: registers itself with a process-lifetime singleton and never unregisters.
 *
 * `onCleared` is where that would happen, and it is not overridden. Every visit to the route
 * creates a new ViewModel and adds one more permanently-reachable object.
 */
class LeakyRegistryViewModel : ViewModel() {
    init {
        GlobalListenerRegistry.register(this)
    }
}

/** CONTROL: same registration, undone in `onCleared`. */
class CleanRegistryViewModel : ViewModel() {
    init {
        GlobalListenerRegistry.register(this)
    }

    override fun onCleared() {
        GlobalListenerRegistry.unregister(this)
        super.onCleared()
    }
}

@Composable
fun ViewModelLeakScreen() {
    val model: LeakyRegistryViewModel = viewModel()
    RegistryScreenBody(
        route = Routes.VIEWMODEL_LEAK,
        fault = "FAULT — ViewModel registers with a global singleton and never unregisters in onCleared()",
        note = "Back out and re-enter: onCleared() runs, but the registry never shrinks.",
        model = model,
    )
}

@Composable
fun ViewModelLeakCleanScreen() {
    val model: CleanRegistryViewModel = viewModel()
    RegistryScreenBody(
        route = Routes.VIEWMODEL_LEAK_CLEAN,
        fault = "CONTROL — same registration, unregistered in onCleared()",
        note = "Back out and re-enter: this screen's entry is removed every time.",
        model = model,
    )
}

@Composable
private fun RegistryScreenBody(route: String, fault: String, note: String, model: ViewModel) {
    var snapshot by remember { mutableStateOf(GlobalListenerRegistry.describe()) }
    var size by remember { mutableStateOf(GlobalListenerRegistry.size) }

    ScreenScaffold(route = route, fault = fault) {
        Text("This screen's ViewModel: @${Integer.toHexString(System.identityHashCode(model))}")
        Spacer(Modifier.height(12.dp))
        Text("GlobalListenerRegistry holds $size: $snapshot")
        Spacer(Modifier.height(12.dp))
        Text(note, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(20.dp))
        Button(onClick = {
            snapshot = GlobalListenerRegistry.describe()
            size = GlobalListenerRegistry.size
        }) {
            Text("Refresh registry count")
        }
    }
}
