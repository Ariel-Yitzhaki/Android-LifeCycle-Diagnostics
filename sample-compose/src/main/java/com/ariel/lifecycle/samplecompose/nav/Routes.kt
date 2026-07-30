package com.ariel.lifecycle.samplecompose.nav

/** One route per screen; the fault is readable from the route name. */
object Routes {
    const val HOME = "home"

    const val SLOW_CREATE = "slow-create"
    const val SLOW_CREATE_CLEAN = "slow-create-clean"

    const val SLOW_RESUME = "slow-resume"
    const val SLOW_RESUME_CLEAN = "slow-resume-clean"

    const val DISK_READ = "main-thread-disk-read"
    const val DISK_READ_CLEAN = "main-thread-disk-read-clean"

    const val ACTIVITY_LEAK = "leaky-activity"
    const val ACTIVITY_LEAK_CLEAN = "leaky-activity-clean"

    const val VIEWMODEL_LEAK = "leaky-viewmodel"
    const val VIEWMODEL_LEAK_CLEAN = "leaky-viewmodel-clean"

    const val UNREGISTERED_RECEIVER = "unregistered-receiver"
    const val UNREGISTERED_RECEIVER_CLEAN = "unregistered-receiver-clean"

    const val JANK_LIST = "jank-list"
    const val JANK_LIST_CLEAN = "jank-list-clean"

    const val RECOMPOSITION_CHURN = "recomposition-churn"
    const val RECOMPOSITION_CHURN_CLEAN = "recomposition-churn-clean"

    const val RELAUNCH_SELF = "relaunch-self"
    const val START_FOR_RESULT = "start-for-result"
    const val SECONDARY_PROCESS = "secondary-process"
}

class RouteEntry(val route: String, val fault: String)

/** Everything the home screen lists, in fault/control pairs. */
object RouteCatalog {

    val entries: List<RouteEntry> = listOf(
        RouteEntry(Routes.SLOW_CREATE, "FAULT — 400ms of real work blocks the first composition"),
        RouteEntry(Routes.SLOW_CREATE_CLEAN, "CONTROL — same work, off the main thread"),
        RouteEntry(Routes.SLOW_RESUME, "FAULT — 400ms of real work blocks every ON_RESUME"),
        RouteEntry(Routes.SLOW_RESUME_CLEAN, "CONTROL — same per-resume work, off the main thread"),
        RouteEntry(Routes.DISK_READ, "FAULT — 4 MiB file read on the main thread during composition"),
        RouteEntry(Routes.DISK_READ_CLEAN, "CONTROL — same read on Dispatchers.IO"),
        RouteEntry(Routes.ACTIVITY_LEAK, "FAULT — Activity stored in a singleton, never cleared"),
        RouteEntry(Routes.ACTIVITY_LEAK_CLEAN, "CONTROL — same field, released on dispose"),
        RouteEntry(Routes.VIEWMODEL_LEAK, "FAULT — ViewModel registers globally, never unregisters"),
        RouteEntry(Routes.VIEWMODEL_LEAK_CLEAN, "CONTROL — same registration, undone in onCleared()"),
        RouteEntry(Routes.UNREGISTERED_RECEIVER, "FAULT — receiver registered on enter, never unregistered"),
        RouteEntry(Routes.UNREGISTERED_RECEIVER_CLEAN, "CONTROL — same receiver, unregistered on dispose"),
        RouteEntry(Routes.JANK_LIST, "FAULT — 12ms of blocking work per row during scroll"),
        RouteEntry(Routes.JANK_LIST_CLEAN, "CONTROL — same rows, computed off the main thread"),
        RouteEntry(Routes.RECOMPOSITION_CHURN, "FAULT — whole subtree recomposes ~60x per second"),
        RouteEntry(Routes.RECOMPOSITION_CHURN_CLEAN, "CONTROL — same ticker, only the leaf recomposes"),
        RouteEntry(Routes.RELAUNCH_SELF, "EXERCISE — finishes and relaunches the Activity 5 times"),
        RouteEntry(Routes.START_FOR_RESULT, "EXERCISE — starts another Activity and comes back"),
        RouteEntry(Routes.SECONDARY_PROCESS, "EXERCISE — launches an Activity in android:process=\":secondary\""),
    )
}
