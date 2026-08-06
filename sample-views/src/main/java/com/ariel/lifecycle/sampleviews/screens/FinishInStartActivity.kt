package com.ariel.lifecycle.sampleviews.screens

import android.os.Bundle
import com.ariel.lifecycle.sampleviews.ui.SimpleScreenActivity

/**
 * FAULT: decides in `onStart` that the user should not be here, and calls `finish()`.
 *
 * The view has already been inflated and laid out by then, so the screen is built, measured and
 * thrown away without the user ever getting to touch it. Deciding the same thing one callback
 * earlier, in `onCreate`, costs nothing and is the fix — which is why the library treats a
 * component that never started as normal and only reports this one.
 *
 * A gate that reads a flag, a session or a feature switch is the usual shape of this bug: the
 * check works, it is just made too late.
 */
class FinishInStartActivity : SimpleScreenActivity() {

    override val faultDescription =
        "FAULT — inflates its view, then decides in onStart() to finish() without ever resuming"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStatus("If you can read this, the screen did not finish.")
        setNote("It should close by itself before it is ever interactive.")
    }

    override fun onStart() {
        super.onStart()
        // Too late. onCreate has run, the layout is up, and the user is about to see a flash of a
        // screen that was never meant for them.
        finish()
    }
}
