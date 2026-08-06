package com.ariel.lifecycle.sampleviews

import android.app.Application
import android.os.StrictMode
import android.util.Log
import android.view.LayoutInflater
import com.ariel.diagnostics.blocking.MainThreadBlocking
import com.ariel.diagnostics.callbacks.CallbackValidation
import com.ariel.diagnostics.leaks.LeakDetection
import com.ariel.diagnostics.lifecycle.LifecycleDiagnostics
import com.ariel.lifecycle.sampleviews.core.SampleFiles
import com.ariel.lifecycle.sampleviews.core.SampleSocket
import com.ariel.lifecycle.sampleviews.ui.HomeRow
import com.ariel.lifecycle.sampleviews.ui.HomeRows
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.concurrent.thread

class SampleViewsApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // The four features are not installed here any more. DiagnosticsInitProvider in the library
        // installs them before this method runs, so an app gets them from the dependency alone. This
        // class is still here for the sample's own setup below.

        permitNetworkViolationsToBeReported()

        // SecondaryProcessActivity starts a second copy of this Application in :secondary, so this
        // method runs in both processes and the two of them want different things.
        if (getProcessName() == packageName) {
            thread(name = "sample-warm-up", isDaemon = true) { warmUp() }
        } else {
            // A provider declared without android:process is only created in the default process, so
            // :secondary installs the features by hand to keep measuring its own Activities. All
            // four, so a screen in this process is watched exactly like one in the main process.
            // Each install() is idempotent, so this stays correct even if the provider does run here.
            LifecycleDiagnostics.install(this)
            CallbackValidation.install(this)
            LeakDetection.install(this)
            MainThreadBlocking.install(this)
        }
    }

    /**
     * Clears Android's own death-on-network penalty, so MainThreadNetworkActivity can be reported
     * instead of killing the process.
     *
     * Android switches `PENALTY_DEATH_ON_NETWORK` on for every app targeting API 10 or later, and
     * StrictMode checks that penalty first: it throws `NetworkOnMainThreadException` and never
     * reaches the listener the library installed. So an app left at the default can never see a
     * NetworkViolation reported — it only ever sees the crash.
     *
     * A fresh Builder starts from an empty mask. The library seeds its own policy from whatever is
     * in force when it installs, which is this one, so its disk and network checks are added on top
     * of a mask with no death penalty in it.
     *
     * Only a sample app should do this. In a real app the crash is the more useful outcome.
     */
    private fun permitNetworkViolationsToBeReported() {
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().build())
    }

    /**
     * Everything the sample wants ready before the first screen opens, all of it off the main
     * thread and none of it something a real app would need.
     *
     * The point is that a CONTROL screen has to be genuinely clean. Without this, the first visit
     * to any screen that uses a coroutine paid for loading the coroutine machinery inside its own
     * onCreate, crossed the library's 50 ms line, and was reported as slow — which is the opposite
     * of what a CONTROL is for.
     */
    private fun warmUp() {
        // The blob the disk-read screens read.
        SampleFiles.ensureSeeded(this)

        // The loopback listener the network screens connect to. Binding is itself a network call,
        // so it has to happen here rather than on whichever thread a screen runs on.
        SampleSocket.ensureListening()

        // Loads Dispatchers, the coroutine builders and the state machine classes, so the first
        // screen to launch a coroutine is not also the first to load them.
        runBlocking { withContext(Dispatchers.Default) { } }

        warmUpLayouts()

        // The home screen's rows: spans, strings and nothing else. Built here rather than in
        // HomeActivity.onCreate for the same reason.
        val built = HomeRows.build()
        HomeRows.prepareFirstRows(built)
        rows = built
    }

    /**
     * Inflates the layouts the screens share, so the first fragment to build a view is not also
     * the one paying to load the layout, its view classes and the resources behind them. Without
     * this a CONTROL screen is reported as slow for a reason that has nothing to do with what it
     * is controlling for.
     *
     * Inflating off the main thread is what AsyncLayoutInflater does, and these layouts are plain
     * enough for it. Wrapped anyway: a warm-up is an optimisation, and one that fails should cost
     * a slower first screen, never a crash.
     */
    private fun warmUpLayouts() {
        try {
            val inflater = LayoutInflater.from(this)
            inflater.inflate(R.layout.fragment_simple, null, false)
            inflater.inflate(R.layout.item_home_screen, null, false)
        } catch (e: RuntimeException) {
            Log.w("SampleViews", "layout warm-up skipped: ${e.javaClass.simpleName}", e)
        }
    }

    companion object {

        // Written on the warm-up thread and read on the main thread, so @Volatile keeps the main
        // thread from seeing a half-built list. Null only if the home screen opens before the
        // warm-up finishes, which is what the fallback below is for.
        @Volatile
        private var rows: List<HomeRow>? = null

        /** The prepared rows, or a fresh build if the warm-up has not finished yet. */
        fun homeRows(): List<HomeRow> = rows ?: HomeRows.build()
    }
}
