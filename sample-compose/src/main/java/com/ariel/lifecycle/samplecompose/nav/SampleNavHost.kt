package com.ariel.lifecycle.samplecompose.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ariel.lifecycle.samplecompose.screens.ActivityLeakCleanScreen
import com.ariel.lifecycle.samplecompose.screens.ActivityLeakScreen
import com.ariel.lifecycle.samplecompose.screens.DiskReadCleanScreen
import com.ariel.lifecycle.samplecompose.screens.DiskReadScreen
import com.ariel.lifecycle.samplecompose.screens.HomeScreen
import com.ariel.lifecycle.samplecompose.screens.JankListCleanScreen
import com.ariel.lifecycle.samplecompose.screens.JankListScreen
import com.ariel.lifecycle.samplecompose.screens.RecompositionChurnCleanScreen
import com.ariel.lifecycle.samplecompose.screens.RecompositionChurnScreen
import com.ariel.lifecycle.samplecompose.screens.RelaunchSelfScreen
import com.ariel.lifecycle.samplecompose.screens.SecondaryProcessScreen
import com.ariel.lifecycle.samplecompose.screens.SlowCreateCleanScreen
import com.ariel.lifecycle.samplecompose.screens.SlowCreateScreen
import com.ariel.lifecycle.samplecompose.screens.SlowResumeCleanScreen
import com.ariel.lifecycle.samplecompose.screens.SlowResumeScreen
import com.ariel.lifecycle.samplecompose.screens.StartForResultScreen
import com.ariel.lifecycle.samplecompose.screens.UnregisteredReceiverCleanScreen
import com.ariel.lifecycle.samplecompose.screens.UnregisteredReceiverScreen
import com.ariel.lifecycle.samplecompose.screens.ViewModelLeakCleanScreen
import com.ariel.lifecycle.samplecompose.screens.ViewModelLeakScreen

@Composable
fun SampleNavHost(startRoute: String?) {
    val navController = rememberNavController()

    // The relaunch screen restarts the Activity with a route in the Intent. Push it on top of home
    // rather than making it the start destination, so Back still leads back to the list.
    var consumedStartRoute by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(startRoute) {
        if (startRoute != null && !consumedStartRoute) {
            consumedStartRoute = true
            navController.navigate(startRoute)
        }
    }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) { HomeScreen(onOpen = navController::navigate) }

        composable(Routes.SLOW_CREATE) { SlowCreateScreen() }
        composable(Routes.SLOW_CREATE_CLEAN) { SlowCreateCleanScreen() }

        composable(Routes.SLOW_RESUME) { SlowResumeScreen() }
        composable(Routes.SLOW_RESUME_CLEAN) { SlowResumeCleanScreen() }

        composable(Routes.DISK_READ) { DiskReadScreen() }
        composable(Routes.DISK_READ_CLEAN) { DiskReadCleanScreen() }

        composable(Routes.ACTIVITY_LEAK) { ActivityLeakScreen() }
        composable(Routes.ACTIVITY_LEAK_CLEAN) { ActivityLeakCleanScreen() }

        composable(Routes.VIEWMODEL_LEAK) { ViewModelLeakScreen() }
        composable(Routes.VIEWMODEL_LEAK_CLEAN) { ViewModelLeakCleanScreen() }

        composable(Routes.UNREGISTERED_RECEIVER) { UnregisteredReceiverScreen() }
        composable(Routes.UNREGISTERED_RECEIVER_CLEAN) { UnregisteredReceiverCleanScreen() }

        composable(Routes.JANK_LIST) { JankListScreen() }
        composable(Routes.JANK_LIST_CLEAN) { JankListCleanScreen() }

        composable(Routes.RECOMPOSITION_CHURN) { RecompositionChurnScreen() }
        composable(Routes.RECOMPOSITION_CHURN_CLEAN) { RecompositionChurnCleanScreen() }

        composable(Routes.RELAUNCH_SELF) { RelaunchSelfScreen() }
        composable(Routes.START_FOR_RESULT) { StartForResultScreen() }
        composable(Routes.SECONDARY_PROCESS) { SecondaryProcessScreen() }
    }
}
