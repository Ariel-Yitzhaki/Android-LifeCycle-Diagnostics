package com.ariel.lifecycle.sampleviews.screens

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.ariel.lifecycle.sampleviews.ui.SimpleScreenActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * EXERCISE (no fault): finishes and relaunches itself [PASSES] times.
 *
 * Drives a burst of destroy/create cycles on a single Activity class without the user having to
 * tap anything. The back stack stays flat because each pass calls `finish()` before starting the
 * replacement, so Back still returns to the home screen.
 */
class RelaunchSelfActivity : SimpleScreenActivity() {

    override val faultDescription =
        "EXERCISE — finishes and relaunches itself $PASSES times to drive repeated destroy/create"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pass = intent.getIntExtra(EXTRA_PASS, 1)
        setStatus("Pass $pass of $PASSES — instance @${Integer.toHexString(System.identityHashCode(this))}")

        if (pass < PASSES) {
            setNote("Relaunching in ${DELAY_MS}ms…")
            lifecycleScope.launch {
                delay(DELAY_MS)
                relaunch(pass + 1)
            }
        } else {
            setNote("Done — $PASSES create/destroy cycles. Back returns to the home screen.")
            primaryButton("Run the loop again") { relaunch(1) }
        }
    }

    private fun relaunch(pass: Int) {
        val next = Intent(this, RelaunchSelfActivity::class.java).putExtra(EXTRA_PASS, pass)
        finish()
        startActivity(next)
    }

    companion object {
        private const val EXTRA_PASS = "pass"
        private const val PASSES = 5
        private const val DELAY_MS = 700L
    }
}
