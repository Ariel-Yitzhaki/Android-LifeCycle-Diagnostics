package com.ariel.lifecycle.sampleviews

import android.content.Context
import android.content.Intent
import com.ariel.lifecycle.sampleviews.fragments.FragmentHostActivity
import com.ariel.lifecycle.sampleviews.fragments.FragmentViewLeakCleanFragment
import com.ariel.lifecycle.sampleviews.fragments.FragmentViewLeakFragment
import com.ariel.lifecycle.sampleviews.fragments.ViewModelLeakCleanFragment
import com.ariel.lifecycle.sampleviews.fragments.ViewModelLeakFragment
import com.ariel.lifecycle.sampleviews.screens.ActivityLeakActivity
import com.ariel.lifecycle.sampleviews.screens.ActivityLeakCleanActivity
import com.ariel.lifecycle.sampleviews.screens.JankListActivity
import com.ariel.lifecycle.sampleviews.screens.JankListCleanActivity
import com.ariel.lifecycle.sampleviews.screens.MainThreadDiskReadActivity
import com.ariel.lifecycle.sampleviews.screens.MainThreadDiskReadCleanActivity
import com.ariel.lifecycle.sampleviews.screens.RelaunchSelfActivity
import com.ariel.lifecycle.sampleviews.screens.SecondaryProcessActivity
import com.ariel.lifecycle.sampleviews.screens.SlowCreateActivity
import com.ariel.lifecycle.sampleviews.screens.SlowCreateCleanActivity
import com.ariel.lifecycle.sampleviews.screens.SlowResumeActivity
import com.ariel.lifecycle.sampleviews.screens.SlowResumeCleanActivity
import com.ariel.lifecycle.sampleviews.screens.StartForResultActivity
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
            "screen adds a [configuration change] note to its timings."

    val categories: List<ScreenCategory> = listOf(

        ScreenCategory(
            title = "1 · Slow lifecycle callbacks",
            tags = "LifecycleDiagnostics",
            explanation =
                "Every lifecycle callback of every screen is timed, and anything over 50 ms is " +
                    "flagged SLOW. Work done inline in onCreate or onResume runs before the screen " +
                    "can draw, so the user waits for all of it. Open a FAULT screen and the warning " +
                    "appears as it opens; the CONTROL beside it does the same work off the main " +
                    "thread and only prints ordinary timings.",
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
                    expect = "no SLOW onCreate. The very first visit can still cross 50 ms while " +
                        "the coroutine classes load, so visit it twice",
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
            ),
        ),

        ScreenCategory(
            title = "2 · Work on the main thread",
            tags = "MainThreadBlocking",
            explanation =
                "Three things are watched here: any single main-thread message running longer " +
                    "than 200 ms, printed with a snapshot of what the thread was doing; the share " +
                    "of frames a screen dropped while the user was on it; and disk or network " +
                    "calls made on the main thread, which StrictMode catches. Every finding names " +
                    "the screen that was in front at the time.",
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
                    name = "JankListActivity",
                    fault = "FAULT — 400 rows, each costing 12 ms of blocking work inside " +
                        "onBindViewHolder(). Scroll hard",
                    expect = "main-thread messages over 200 ms while you scroll, with " +
                        "BusyWork.spin on the stack. The dropped-frame percentage is unreliable " +
                        "on an emulator — check that half on a physical device",
                ) { Intent(it, JankListActivity::class.java) },

                screen(
                    name = "JankListCleanActivity",
                    fault = "CONTROL — the same rows, computed on Dispatchers.Default and cached",
                    expect = "rows that read \"computing…\" and then fill in, and no main-thread " +
                        "message over 200 ms while scrolling",
                ) { Intent(it, JankListCleanActivity::class.java) },
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
                    "StrictMode's VM checks give a second opinion under CallbackValidation.",
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
                    name = "ViewModelLeakFragment",
                    fault = "FAULT — its ViewModel registers with a process-lifetime singleton and " +
                        "never unregisters",
                    expect = "nothing, and that is the lesson. The count on the screen climbs on " +
                        "every visit, but the library watches Activities, Fragments and Fragment " +
                        "views — a leaked ViewModel holding none of them is outside what it can see",
                ) { FragmentHostActivity.intent(it, ViewModelLeakFragment::class.java) },

                screen(
                    name = "ViewModelLeakCleanFragment",
                    fault = "CONTROL — the same registration, undone in onCleared()",
                    expect = "nothing here either. The difference between this screen and the one " +
                        "above shows on the device, in the registry count, not in Logcat",
                ) { FragmentHostActivity.intent(it, ViewModelLeakCleanFragment::class.java) },

                screen(
                    name = "UnregisteredReceiverActivity",
                    fault = "FAULT — registers a BroadcastReceiver in onStart() and never " +
                        "unregisters it",
                    expect = "after four visits, \"UnregisteredReceiverActivity (Activity) was " +
                        "still in memory…\". The receiver holds the Activity, so the Activity is " +
                        "what gets reported",
                ) { Intent(it, UnregisteredReceiverActivity::class.java) },

                screen(
                    name = "UnregisteredReceiverCleanActivity",
                    fault = "CONTROL — the same receiver, unregistered in onStop()",
                    expect = "registrations and unregistrations staying equal on screen, and " +
                        "nothing under LeakDetection",
                ) { Intent(it, UnregisteredReceiverCleanActivity::class.java) },
            ),
        ),

        ScreenCategory(
            title = "4 · Lifecycle order and shape",
            tags = "CallbackValidation",
            explanation =
                "A small state machine follows every live Activity and Fragment and reports a " +
                    "step the lifecycle should not take: a screen started and then destroyed " +
                    "without ever reaching resumed, onStart and onStop counts that do not " +
                    "balance, a Fragment destroyed while its view is still around, or one " +
                    "Activity class destroyed and recreated more than three times in ten " +
                    "seconds, which is a restart loop.",
            screens = listOf(
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
                    expect = "the same tags arriving from a different pid. Compare the pid shown " +
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
