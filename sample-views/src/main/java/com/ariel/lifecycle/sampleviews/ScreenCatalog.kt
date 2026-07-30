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

class SampleScreen(
    val name: String,
    val fault: String,
    val createIntent: (Context) -> Intent,
)

/** Everything [HomeActivity] lists, in fault/control pairs. */
object ScreenCatalog {

    val screens: List<SampleScreen> = listOf(
        activity("SlowCreateActivity", "FAULT — 400ms of real work inline in onCreate()") {
            Intent(it, SlowCreateActivity::class.java)
        },
        activity("SlowCreateCleanActivity", "CONTROL — same work, off the main thread") {
            Intent(it, SlowCreateCleanActivity::class.java)
        },
        activity("SlowResumeActivity", "FAULT — 400ms of real work inline in onResume()") {
            Intent(it, SlowResumeActivity::class.java)
        },
        activity("SlowResumeCleanActivity", "CONTROL — same per-resume work, off the main thread") {
            Intent(it, SlowResumeCleanActivity::class.java)
        },
        activity("MainThreadDiskReadActivity", "FAULT — 4 MiB file read on the main thread in onCreate()") {
            Intent(it, MainThreadDiskReadActivity::class.java)
        },
        activity("MainThreadDiskReadCleanActivity", "CONTROL — same read on Dispatchers.IO") {
            Intent(it, MainThreadDiskReadCleanActivity::class.java)
        },
        activity("ActivityLeakActivity", "FAULT — Activity stored in a companion field, never cleared") {
            Intent(it, ActivityLeakActivity::class.java)
        },
        activity("ActivityLeakCleanActivity", "CONTROL — same field, cleared in onDestroy()") {
            Intent(it, ActivityLeakCleanActivity::class.java)
        },
        activity("FragmentViewLeakFragment", "FAULT — view binding never nulled in onDestroyView()") {
            FragmentHostActivity.intent(it, FragmentViewLeakFragment::class.java)
        },
        activity("FragmentViewLeakCleanFragment", "CONTROL — binding nulled in onDestroyView()") {
            FragmentHostActivity.intent(it, FragmentViewLeakCleanFragment::class.java)
        },
        activity("ViewModelLeakFragment", "FAULT — ViewModel registers globally, never unregisters") {
            FragmentHostActivity.intent(it, ViewModelLeakFragment::class.java)
        },
        activity("ViewModelLeakCleanFragment", "CONTROL — same registration, undone in onCleared()") {
            FragmentHostActivity.intent(it, ViewModelLeakCleanFragment::class.java)
        },
        activity("UnregisteredReceiverActivity", "FAULT — receiver registered in onStart(), never unregistered") {
            Intent(it, UnregisteredReceiverActivity::class.java)
        },
        activity("UnregisteredReceiverCleanActivity", "CONTROL — same receiver, unregistered in onStop()") {
            Intent(it, UnregisteredReceiverCleanActivity::class.java)
        },
        activity("JankListActivity", "FAULT — 12ms of blocking work per row during scroll") {
            Intent(it, JankListActivity::class.java)
        },
        activity("JankListCleanActivity", "CONTROL — same rows, computed off the main thread") {
            Intent(it, JankListCleanActivity::class.java)
        },
        activity("RelaunchSelfActivity", "EXERCISE — finishes and relaunches itself 5 times") {
            Intent(it, RelaunchSelfActivity::class.java)
        },
        activity("StartForResultActivity", "EXERCISE — starts another Activity and comes back") {
            Intent(it, StartForResultActivity::class.java)
        },
        activity("SecondaryProcessActivity", "EXERCISE — runs in android:process=\":secondary\"") {
            Intent(it, SecondaryProcessActivity::class.java)
        },
    )

    private fun activity(name: String, fault: String, createIntent: (Context) -> Intent) =
        SampleScreen(name, fault, createIntent)
}
