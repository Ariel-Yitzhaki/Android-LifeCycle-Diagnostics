package com.ariel.lifecycle.sampleviews.screens

import android.annotation.SuppressLint
import android.os.Bundle
import com.ariel.lifecycle.sampleviews.ui.SimpleScreenActivity

/**
 * CONTROL: the same "current screen" pointer, released in `onDestroy`.
 *
 * At most one live Activity is referenced at a time, and nothing survives the screen going away.
 */
class ActivityLeakCleanActivity : SimpleScreenActivity() {

    override val faultDescription =
        "CONTROL — same companion-object pointer, cleared in onDestroy()"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        current = this
        visits++

        setStatus("Activity instances retained by the companion object: ${if (current == null) 0 else 1}")
        setNote(
            "This instance: @${Integer.toHexString(System.identityHashCode(this))}\n" +
                "Visits so far: $visits (a plain Int — it holds no Activity)."
        )
    }

    override fun onDestroy() {
        // The whole fix: stop pointing at an Activity that is on its way out.
        if (current === this) current = null
        super.onDestroy()
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var current: ActivityLeakCleanActivity? = null

        private var visits = 0
    }
}
