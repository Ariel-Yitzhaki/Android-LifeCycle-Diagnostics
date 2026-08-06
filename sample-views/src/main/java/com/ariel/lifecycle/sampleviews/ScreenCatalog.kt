package com.ariel.lifecycle.sampleviews

import android.content.Context
import android.content.Intent
import com.ariel.lifecycle.sampleviews.fragments.FragmentHostActivity
import com.ariel.lifecycle.sampleviews.fragments.FragmentViewLeakCleanFragment
import com.ariel.lifecycle.sampleviews.fragments.FragmentViewLeakFragment
import com.ariel.lifecycle.sampleviews.fragments.NestedParentFragment
import com.ariel.lifecycle.sampleviews.fragments.SlowViewBuildCleanFragment
import com.ariel.lifecycle.sampleviews.fragments.SlowViewBuildFragment
import com.ariel.lifecycle.sampleviews.fragments.ViewCaptureCleanFragment
import com.ariel.lifecycle.sampleviews.fragments.ViewCaptureFragment
import com.ariel.lifecycle.sampleviews.fragments.ViewModelLeakCleanFragment
import com.ariel.lifecycle.sampleviews.fragments.ViewModelLeakFragment
import com.ariel.lifecycle.sampleviews.fragments.ViewWithoutContainerFragment
import com.ariel.lifecycle.sampleviews.core.HeavyRows
import com.ariel.lifecycle.sampleviews.screens.ActivityLeakActivity
import com.ariel.lifecycle.sampleviews.screens.ActivityLeakCleanActivity
import com.ariel.lifecycle.sampleviews.screens.BusySettleActivity
import com.ariel.lifecycle.sampleviews.screens.BusySettleCleanActivity
import com.ariel.lifecycle.sampleviews.screens.FinishInStartActivity
import com.ariel.lifecycle.sampleviews.screens.JankDialogActivity
import com.ariel.lifecycle.sampleviews.screens.JankListActivity
import com.ariel.lifecycle.sampleviews.screens.JankListCleanActivity
import com.ariel.lifecycle.sampleviews.screens.LeakedClosableActivity
import com.ariel.lifecycle.sampleviews.screens.LeakedClosableCleanActivity
import com.ariel.lifecycle.sampleviews.screens.MainThreadDiskReadActivity
import com.ariel.lifecycle.sampleviews.screens.MainThreadDiskReadCleanActivity
import com.ariel.lifecycle.sampleviews.screens.MainThreadDiskWriteActivity
import com.ariel.lifecycle.sampleviews.screens.MainThreadDiskWriteCleanActivity
import com.ariel.lifecycle.sampleviews.screens.MainThreadNetworkActivity
import com.ariel.lifecycle.sampleviews.screens.MainThreadNetworkCleanActivity
import com.ariel.lifecycle.sampleviews.screens.RelaunchSelfActivity
import com.ariel.lifecycle.sampleviews.screens.SecondaryProcessActivity
import com.ariel.lifecycle.sampleviews.screens.ServiceBindLeakActivity
import com.ariel.lifecycle.sampleviews.screens.ServiceBindLeakCleanActivity
import com.ariel.lifecycle.sampleviews.screens.SlowCreateActivity
import com.ariel.lifecycle.sampleviews.screens.SlowCreateCleanActivity
import com.ariel.lifecycle.sampleviews.screens.SlowResumeActivity
import com.ariel.lifecycle.sampleviews.screens.SlowResumeCleanActivity
import com.ariel.lifecycle.sampleviews.screens.StartForResultActivity
import com.ariel.lifecycle.sampleviews.screens.StrayCallbackActivity
import com.ariel.lifecycle.sampleviews.screens.UnregisteredReceiverActivity
import com.ariel.lifecycle.sampleviews.screens.UnregisteredReceiverCleanActivity

/**
 * One screen in the sample, as the home screen lists it.
 *
 * [fault] says what the screen does to itself. [expect] says what the library should print because
 * of it, which is the half that tells you whether the run worked.
 */
class SampleScreen(
    val name: String,
    val fault: String,
    val expect: String,
    val createIntent: (Context) -> Intent,
)

/**
 * A group of screens that exercise the same library feature, with the explanation of what that
 * feature watches for.
 */
class ScreenCategory(
    val title: String,
    val tags: String,
    val explanation: String,
    val screens: List<SampleScreen>,
)

/** Everything [HomeActivity] lists, grouped by the library feature each group exercises. */
object ScreenCatalog {

    /** Printed once at the top of the home screen, above the first group. */
    const val LEGEND: String =
        "Every screen below plants one problem, or deliberately avoids it.\n\n" +
            "FAULT does something wrong, and the library should say so.\n" +
            "CONTROL does the same job correctly, and the library should stay quiet.\n" +
            "EXERCISE plants nothing, and only drives a lifecycle worth measuring.\n\n" +
            "Open a screen, then read Logcat filtered to the library's four tags:\n" +
            "tag:LifecycleDiagnostics | tag:CallbackValidation | tag:LeakDetection | " +
            "tag:MainThreadBlocking\n\n" +
            "Warn lines (W) are findings. Debug lines (D) are ordinary measurements. Rotating any " +
            "screen adds a [configuration change] note to its timings. Leak findings need a class " +
            "destroyed at least three times, so open those screens four times and wait a few " +
            "seconds."

    val categories: List<ScreenCategory> = listOf(

        ScreenCategory(
            title = "1 · Slow lifecycle callbacks",
            tags = "LifecycleDiagnostics",
            explanation =
                "Every lifecycle callback of every screen is timed, and anything over 50 ms is " +
                    "flagged SLOW. Work done inline in onCreate, onResume or a fragment's view " +
                    "building runs before the screen can draw, so the user waits for all of it. " +
                    "Open a FAULT screen and the warning appears as it opens; the CONTROL beside " +
                    "it does the same work off the main thread and only prints ordinary timings.",
            screens = listOf(
                screen(
                    name = "SlowCreateActivity",
                    fault = "FAULT — 400 ms of real CPU work, inline on the main thread, in onCreate()",
                    expect = "SlowCreateActivity.onCreate took ~400 ms SLOW, plus a main-thread " +
                        "message over 200 ms whose stack names BusyWork.spin",
                ) { Intent(it, SlowCreateActivity::class.java) },

                screen(
                    name = "SlowCreateCleanActivity",
                    fault = "CONTROL — the same 400 ms, moved to a background dispatcher",
                    expect = "no SLOW onCreate, on the first visit as well as later ones",
                ) { Intent(it, SlowCreateCleanActivity::class.java) },

                screen(
                    name = "SlowResumeActivity",
                    fault = "FAULT — the same 400 ms, but in onResume(), so it is paid again on " +
                        "every return to the screen",
                    expect = "a SLOW onResume each time the screen comes back. Use the button to " +
                        "leave and return, and watch the same warning repeat",
                ) { Intent(it, SlowResumeActivity::class.java) },

                screen(
                    name = "SlowResumeCleanActivity",
                    fault = "CONTROL — the same per-resume work off the main thread, cancelled on pause",
                    expect = "onResume timings in single-digit milliseconds, however many times " +
                        "you leave and return",
                ) { Intent(it, SlowResumeCleanActivity::class.java) },

                screen(
                    name = "SlowViewBuildFragment",
                    fault = "FAULT — ${SlowViewBuildFragment.INFLATE_MS} ms in onCreateView() and " +
                        "${SlowViewBuildFragment.WIRE_UP_MS} ms in onViewCreated()",
                    expect = "\"SlowViewBuildFragment took ~" +
                        "${SlowViewBuildFragment.INFLATE_MS + SlowViewBuildFragment.WIRE_UP_MS} ms " +
                        "to build its view SLOW\" — one number covering both callbacks, because a " +
                        "fragment's view is not finished until the second returns",
                ) { FragmentHostActivity.intent(it, SlowViewBuildFragment::class.java) },

                screen(
                    name = "SlowViewBuildCleanFragment",
                    fault = "CONTROL — the same work, done off the main thread once the view exists",
                    expect = "a view-build time around a tenth of the FAULT screen's. The first " +
                        "fragment opened in a process still pays to load the fragment machinery " +
                        "and can cross 50 ms on its own, so judge this one on the second visit",
                ) { FragmentHostActivity.intent(it, SlowViewBuildCleanFragment::class.java) },

                screen(
                    name = "NestedParentFragment",
                    fault = "EXERCISE — a parent fragment hosting a slow child in its own child " +
                        "FragmentManager",
                    expect = "timings for NestedChildFragment under its own name, including a SLOW " +
                        "view build. A watcher registered only on the Activity's FragmentManager " +
                        "would report none of them",
                ) { FragmentHostActivity.intent(it, NestedParentFragment::class.java) },
            ),
        ),

        ScreenCategory(
            title = "2 · Work on the main thread",
            tags = "MainThreadBlocking",
            explanation =
                "Four things are watched here: any single main-thread message running longer than " +
                    "200 ms, printed with a snapshot of what the thread was doing; a screen that " +
                    "keeps the main thread busy for most of its first five seconds, however small " +
                    "the individual messages; the share of frames a screen dropped while the user " +
                    "was on it; and disk or network calls made on the main thread, which " +
                    "StrictMode catches. Every finding names the screen that was in front.",
            screens = listOf(
                screen(
                    name = "MainThreadDiskReadActivity",
                    fault = "FAULT — reads a 4 MiB file synchronously on the main thread in onCreate()",
                    expect = "a run of StrictMode DiskReadViolation lines, each one ending at the " +
                        "line of SampleFiles that touched the disk, plus a SLOW onCreate",
                ) { Intent(it, MainThreadDiskReadActivity::class.java) },

                screen(
                    name = "MainThreadDiskReadCleanActivity",
                    fault = "CONTROL — the same read, moved to Dispatchers.IO",
                    expect = "nothing under this tag. The screen paints straight away and fills " +
                        "in the receipt when the read finishes",
                ) { Intent(it, MainThreadDiskReadCleanActivity::class.java) },

                screen(
                    name = "MainThreadDiskWriteActivity",
                    fault = "FAULT — writes and fsyncs 2 MiB on the main thread in onCreate()",
                    expect = "StrictMode DiskWriteViolation lines naming SampleFiles.writeBlocking. " +
                        "Writes are the half of disk I/O that feels like it can be fired and " +
                        "forgotten; the thread still waits",
                ) { Intent(it, MainThreadDiskWriteActivity::class.java) },

                screen(
                    name = "MainThreadDiskWriteCleanActivity",
                    fault = "CONTROL — the same write, moved to Dispatchers.IO",
                    expect = "nothing under this tag",
                ) { Intent(it, MainThreadDiskWriteCleanActivity::class.java) },

                screen(
                    name = "MainThreadNetworkActivity",
                    fault = "FAULT — opens a TCP connection on the main thread in onCreate()",
                    expect = "a StrictMode NetworkViolation naming SampleSocket.connectBlocking. " +
                        "Loopback only, so it works with no network and nothing leaves the device",
                ) { Intent(it, MainThreadNetworkActivity::class.java) },

                screen(
                    name = "MainThreadNetworkCleanActivity",
                    fault = "CONTROL — the same connection, moved to Dispatchers.IO",
                    expect = "nothing under this tag",
                ) { Intent(it, MainThreadNetworkCleanActivity::class.java) },

                screen(
                    name = "BusySettleActivity",
                    fault = "FAULT — 60 ms of main-thread work every 80 ms, for six seconds",
                    expect = "\"kept the main thread busy for ~75% of the first 5000 ms it was in " +
                        "front\". No message here is slow and no callback is either — this is the " +
                        "one the other two detectors cannot see",
                ) { Intent(it, BusySettleActivity::class.java) },

                screen(
                    name = "BusySettleCleanActivity",
                    fault = "CONTROL — the same chunks and the same total work, on Dispatchers.Default",
                    expect = "no busy finding. The main thread only ever draws the results",
                ) { Intent(it, BusySettleCleanActivity::class.java) },

                screen(
                    name = "JankListActivity",
                    fault = "FAULT — ${HeavyRows.ROW_COUNT} rows, each costing ${HeavyRows.COST_MS} ms " +
                        "of blocking work inside onBindViewHolder(). Scroll hard",
                    expect = "main-thread messages over 200 ms while you scroll, with " +
                        "BusyWork.spin on the stack. The dropped-frame percentage often reads 0% " +
                        "on an emulator, because a frame that is always late gets a later " +
                        "deadline to match — check that half on a physical device, or use " +
                        "JankDialogActivity, which trips it here",
                ) { Intent(it, JankListActivity::class.java) },

                screen(
                    name = "JankListCleanActivity",
                    fault = "CONTROL — the same rows, computed on Dispatchers.Default and cached",
                    expect = "rows that read \"computing…\" and then fill in, and no main-thread " +
                        "message over 200 ms while scrolling",
                ) { Intent(it, JankListCleanActivity::class.java) },

                screen(
                    name = "JankDialogActivity",
                    fault = "EXERCISE — puts the same heavy list inside a DialogFragment",
                    expect = "a dropped-frame percentage and a busy-thread finding, both naming " +
                        "JankDialogFragment rather than the Activity under it. This is the " +
                        "frame-drop fixture that works on an emulator: a dialog's first layout " +
                        "blows one deadline outright, where a long scroll only stretches them",
                ) { Intent(it, JankDialogActivity::class.java) },
            ),
        ),

        ScreenCategory(
            title = "3 · Screens that never go away",
            tags = "LeakDetection · CallbackValidation",
            explanation =
                "A destroyed screen is held only through a weak reference. Five seconds later the " +
                    "library asks for a garbage collection and checks whether it survived. One " +
                    "survivor proves nothing, so nothing is printed until the same class has been " +
                    "destroyed three times with at least half of them still in memory: open a " +
                    "FAULT screen, press Back, and repeat four times, then wait a few seconds. " +
                    "StrictMode's VM checks — leaked registration objects, leaked closables and " +
                    "Activity leaks — give a second opinion under CallbackValidation.",
            screens = listOf(
                screen(
                    name = "ActivityLeakActivity",
                    fault = "FAULT — parks `this` in a companion-object field that is never cleared",
                    expect = "after four visits, \"ActivityLeakActivity (Activity) was still in " +
                        "memory 3 of 3 times…\", and a StrictMode InstanceCountViolation under " +
                        "CallbackValidation",
                ) { Intent(it, ActivityLeakActivity::class.java) },

                screen(
                    name = "ActivityLeakCleanActivity",
                    fault = "CONTROL — the same companion-object pointer, cleared in onDestroy()",
                    expect = "silence under LeakDetection, however many times you visit",
                ) { Intent(it, ActivityLeakCleanActivity::class.java) },

                screen(
                    name = "FragmentViewLeakFragment",
                    fault = "FAULT — keeps its view binding in a field and never nulls it in " +
                        "onDestroyView()",
                    expect = "after covering and returning four times, \"(Fragment view, fragment " +
                        "still alive) was still in memory…\" — the back-stack case, where the view " +
                        "is gone but the fragment still points at it",
                ) { FragmentHostActivity.intent(it, FragmentViewLeakFragment::class.java) },

                screen(
                    name = "FragmentViewLeakCleanFragment",
                    fault = "CONTROL — the same binding, nulled in onDestroyView()",
                    expect = "silence, however many times you cover the fragment and come back",
                ) { FragmentHostActivity.intent(it, FragmentViewLeakCleanFragment::class.java) },

                screen(
                    name = "ViewCaptureFragment",
                    fault = "FAULT — hands its root view to a process-lifetime cache",
                    expect = "after four taps of its button, two findings: the view is reported, " +
                        "and so is the fragment. Keeping a fragment's root view keeps the " +
                        "fragment too — androidx tags that view with the fragment that made it — " +
                        "and the view holds its Context, so the Activity goes with them",
                ) { FragmentHostActivity.intent(it, ViewCaptureFragment::class.java) },

                screen(
                    name = "ViewCaptureCleanFragment",
                    fault = "CONTROL — caches the state the view was showing, never the view",
                    expect = "silence. A String holds no Context, so it can live for the session",
                ) { FragmentHostActivity.intent(it, ViewCaptureCleanFragment::class.java) },

                screen(
                    name = "ViewModelLeakFragment",
                    fault = "FAULT — its ViewModel registers with a global singleton, never " +
                        "unregisters, and holds a callback into the fragment",
                    expect = "after four taps of its button, \"ViewModelLeakFragment (Fragment) " +
                        "was still in memory…\". The chain is registry → ViewModel → lambda → " +
                        "fragment; the library names the end of it, not the middle",
                ) { FragmentHostActivity.intent(it, ViewModelLeakFragment::class.java) },

                screen(
                    name = "ViewModelLeakCleanFragment",
                    fault = "CONTROL — the same registration and callback, both dropped in onCleared()",
                    expect = "silence, and a registry count on screen that never grows",
                ) { FragmentHostActivity.intent(it, ViewModelLeakCleanFragment::class.java) },

                screen(
                    name = "UnregisteredReceiverActivity",
                    fault = "FAULT — registers a BroadcastReceiver in onStart() and never " +
                        "unregisters it",
                    expect = "after four visits, \"UnregisteredReceiverActivity (Activity) was " +
                        "still in memory…\". The receiver holds the Activity, so the Activity is " +
                        "what gets reported — Android 16 does not raise its own leaked-receiver " +
                        "complaint here, which is exactly why watching the screen is worth doing",
                ) { Intent(it, UnregisteredReceiverActivity::class.java) },

                screen(
                    name = "UnregisteredReceiverCleanActivity",
                    fault = "CONTROL — the same receiver, unregistered in onStop()",
                    expect = "registrations and unregistrations staying equal on screen, and " +
                        "nothing under LeakDetection",
                ) { Intent(it, UnregisteredReceiverCleanActivity::class.java) },

                screen(
                    name = "ServiceBindLeakActivity",
                    fault = "FAULT — binds to a Service in onStart() and never unbinds",
                    expect = "after four visits, \"ServiceBindLeakActivity (Activity) was still " +
                        "in memory…\" — the connection is an inner class, so it holds the screen. " +
                        "StrictMode's own leaked-registration check is switched on for this too, " +
                        "but Android 16 does not raise it for an outstanding binding",
                ) { Intent(it, ServiceBindLeakActivity::class.java) },

                screen(
                    name = "ServiceBindLeakCleanActivity",
                    fault = "CONTROL — the same bind, undone in onStop()",
                    expect = "binds and unbinds staying equal on screen, and nothing under either tag",
                ) { Intent(it, ServiceBindLeakCleanActivity::class.java) },

                screen(
                    name = "LeakedClosableActivity",
                    fault = "FAULT — opens a FileInputStream and abandons it without closing",
                    expect = "a StrictMode LeakedClosableObject finding naming the line that " +
                        "opened the stream. It arrives from the finalizer, so it is late by " +
                        "design — the screen asks for a collection to bring it forward",
                ) { Intent(it, LeakedClosableActivity::class.java) },

                screen(
                    name = "LeakedClosableCleanActivity",
                    fault = "CONTROL — the same stream, closed by use {}",
                    expect = "nothing, even though this screen asks for the same collection",
                ) { Intent(it, LeakedClosableCleanActivity::class.java) },
            ),
        ),

        ScreenCategory(
            title = "4 · Lifecycle order and shape",
            tags = "CallbackValidation",
            explanation =
                "A small state machine follows every live Activity and Fragment and reports a " +
                    "step the lifecycle should not take: a screen started and then destroyed " +
                    "without ever reaching resumed, a callback arriving from a state it cannot " +
                    "follow, onStart and onStop counts that do not balance, a Fragment destroyed " +
                    "with a view that was never torn down, or one Activity class destroyed and " +
                    "recreated more than three times in ten seconds, which is a restart loop.",
            screens = listOf(
                screen(
                    name = "FinishInStartActivity",
                    fault = "FAULT — inflates its view, then decides in onStart() to finish()",
                    expect = "\"was started and then destroyed without ever reaching resumed, so " +
                        "its view was built for a screen the user never got to use\". Deciding " +
                        "the same thing in onCreate costs nothing and is the fix",
                ) { Intent(it, FinishInStartActivity::class.java) },

                screen(
                    name = "StrayCallbackActivity",
                    fault = "FAULT — calls the framework's own onStart() a second time, by hand",
                    expect = "\"received onStart while it was RESUMED\" on each tap, and then, " +
                        "when you press Back, a second finding about onStarts outnumbering onStops",
                ) { Intent(it, StrayCallbackActivity::class.java) },

                screen(
                    name = "ViewWithoutContainerFragment",
                    fault = "EXERCISE — added with add(fragment, tag) and still inflates a view " +
                        "from onCreateView()",
                    expect = "nothing, and that is the answer. This is as close as app code gets " +
                        "to a view created without a matching onDestroyView; androidx pairs the " +
                        "two on every code path, so the check is for a host that gets it wrong. " +
                        "Nothing appears on screen — the view has nowhere to go",
                ) {
                    FragmentHostActivity.intent(
                        it,
                        ViewWithoutContainerFragment::class.java,
                        withoutContainer = true,
                    )
                },

                screen(
                    name = "RelaunchSelfActivity",
                    fault = "EXERCISE — finishes and relaunches itself five times, about 700 ms apart",
                    expect = "on the fourth pass, \"was destroyed and recreated 4 times in the " +
                        "last 10 seconds… This looks like a restart loop\"",
                ) { Intent(it, RelaunchSelfActivity::class.java) },

                screen(
                    name = "StartForResultActivity",
                    fault = "EXERCISE — a clean round trip to a second Activity and back",
                    expect = "no warnings at all. The timings show an orderly pause and stop on " +
                        "the way out, start and resume on the way back — this is what healthy " +
                        "looks like",
                ) { Intent(it, StartForResultActivity::class.java) },
            ),
        ),

        ScreenCategory(
            title = "5 · A second process",
            tags = "all four tags, a second pid",
            explanation =
                "Everything above runs in the app's main process. A screen declared with " +
                    "android:process=\":secondary\" gets a second Application object, a second " +
                    "copy of every singleton and a main thread of its own, so the library starts " +
                    "over there and reports separately.",
            screens = listOf(
                screen(
                    name = "SecondaryProcessActivity",
                    fault = "EXERCISE — runs in its own process (android:process=\":secondary\")",
                    expect = "all four tags arriving from a different pid. Compare the pid shown " +
                        "on that screen with the one at the top of this one",
                ) { Intent(it, SecondaryProcessActivity::class.java) },
            ),
        ),
    )

    /** Every screen in every category, in the order the home screen lists them. */
    val screens: List<SampleScreen> = categories.flatMap { it.screens }

    /** What the library should print for a screen, looked up by its simple class name. */
    fun expectationFor(screenName: String): String? =
        screens.firstOrNull { it.name == screenName }?.expect

    private fun screen(
        name: String,
        fault: String,
        expect: String,
        createIntent: (Context) -> Intent,
    ) = SampleScreen(name, fault, expect, createIntent)
}
