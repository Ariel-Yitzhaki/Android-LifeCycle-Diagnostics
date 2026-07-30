package com.ariel.lifecycle.sampleviews.screens

import android.app.Application
import android.os.Bundle
import android.os.Process
import com.ariel.lifecycle.sampleviews.core.GlobalListenerRegistry
import com.ariel.lifecycle.sampleviews.ui.SimpleScreenActivity

/**
 * EXERCISE (no fault): declared `android:process=":secondary"` in the manifest.
 *
 * A second Application instance, a second set of singletons, a second main looper. Compare the pid
 * shown here with the one on the home screen.
 */
class SecondaryProcessActivity : SimpleScreenActivity() {

    override val faultDescription =
        "EXERCISE — runs in its own process (android:process=\":secondary\")"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStatus("pid ${Process.myPid()} — process ${Application.getProcessName()}")
        setNote(
            "This process has its own copy of every singleton. GlobalListenerRegistry here holds " +
                "${GlobalListenerRegistry.size} entries, independent of the main process."
        )
    }
}
