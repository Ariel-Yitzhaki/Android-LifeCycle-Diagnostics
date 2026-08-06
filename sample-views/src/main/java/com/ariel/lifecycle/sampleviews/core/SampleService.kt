package com.ariel.lifecycle.sampleviews.core

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

/**
 * A service that does nothing, so that binding to it is the only thing a screen is demonstrating.
 *
 * A ServiceConnection is a registration object: bind from an Activity and never unbind, and the
 * framework notices when that Activity's context is torn down.
 */
class SampleService : Service() {

    private val binder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder = binder

    class LocalBinder : Binder()
}
