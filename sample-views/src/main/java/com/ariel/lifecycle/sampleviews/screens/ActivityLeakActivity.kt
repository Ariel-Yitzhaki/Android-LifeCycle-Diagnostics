package com.ariel.lifecycle.sampleviews.screens

import android.annotation.SuppressLint
import android.os.Bundle
import com.ariel.lifecycle.sampleviews.ui.SimpleScreenActivity

/**
 * FAULT: parks `this` in a companion-object field and never clears it.
 *
 * Every visit and every rotation adds another destroyed Activity — with its whole view hierarchy —
 * to a collection that lives as long as the process.
 */
class ActivityLeakActivity : SimpleScreenActivity() {

    override val faultDescription =
        "FAULT — stores `this` in a companion-object field that is never cleared"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        retain(this)

        setStatus("Activity instances retained by the companion object: ${retainedInstances.size}")
        setNote(
            "This instance: @${Integer.toHexString(System.identityHashCode(this))}\n" +
                "Rotate the device or re-enter this screen — the count only ever goes up."
        )
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        var lastInstance: ActivityLeakActivity? = null
            private set

        @SuppressLint("StaticFieldLeak")
        private val instances = mutableListOf<ActivityLeakActivity>()

        val retainedInstances: List<ActivityLeakActivity> get() = instances

        private fun retain(activity: ActivityLeakActivity) {
            lastInstance = activity
            instances += activity
        }
    }
}
