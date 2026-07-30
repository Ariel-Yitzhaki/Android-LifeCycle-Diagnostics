package com.ariel.lifecycle.sampleviews

import android.app.Application
import com.ariel.diagnostics.callbacks.CallbackValidation
import com.ariel.diagnostics.leaks.LeakDetection
import com.ariel.diagnostics.lifecycle.LifecycleDiagnostics
import com.ariel.lifecycle.sampleviews.core.SampleFiles
import kotlin.concurrent.thread

class SampleViewsApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // The three features are not installed here any more. DiagnosticsInitProvider in the library
        // installs them before this method runs, so an app gets them from the dependency alone. This
        // class is still here for the sample's own setup below.

        // SecondaryProcessActivity starts a second copy of this Application in :secondary, so this
        // method runs in both processes and the two of them want different things.
        if (getProcessName() == packageName) {
            // Seed the blob the disk-read screens read, off the main thread, main process only.
            thread(name = "sample-blob-seed", isDaemon = true) { SampleFiles.ensureSeeded(this) }
        } else {
            // A provider declared without android:process is only created in the default process, so
            // :secondary installs the features by hand to keep measuring its own Activities. Each
            // install() is idempotent, so this stays correct even if the provider does run here.
            LifecycleDiagnostics.install(this)
            CallbackValidation.install(this)
            LeakDetection.install(this)
        }
    }
}
