package com.ariel.lifecycle.sampleviews.screens

import android.os.Bundle
import com.ariel.lifecycle.sampleviews.fragments.JankDialogFragment
import com.ariel.lifecycle.sampleviews.ui.SimpleScreenActivity

/**
 * EXERCISE: a plain screen whose only job is to put [JankDialogFragment] on top of itself.
 *
 * The Activity plants nothing. Everything worth measuring happens in the dialog's window, which is
 * the point: two windows, two counters, and findings that name the dialog rather than the screen
 * underneath it.
 */
class JankDialogActivity : SimpleScreenActivity() {

    override val faultDescription =
        "EXERCISE — shows JankDialogFragment, which drops frames in a window of its own"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStatus("This Activity plants nothing.")
        setNote(
            "The dialog on top of it does. Fling the list inside the dialog, then dismiss it — " +
                "the dialog's frames are counted separately from this screen's."
        )
        primaryButton("Show the janky dialog") { showDialog() }

        if (savedInstanceState == null) {
            showDialog()
        }
    }

    private fun showDialog() {
        if (supportFragmentManager.findFragmentByTag(JankDialogFragment.TAG) == null) {
            JankDialogFragment().show(supportFragmentManager, JankDialogFragment.TAG)
        }
    }
}
