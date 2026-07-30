package com.ariel.lifecycle.sampleviews.screens

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import com.ariel.lifecycle.sampleviews.ui.SimpleScreenActivity

/**
 * EXERCISE (no fault): starts [ResultProviderActivity] and handles its result.
 *
 * Gives the diagnostics library a clean stop → start → resume round trip between two Activities.
 */
class StartForResultActivity : SimpleScreenActivity() {

    private var trips = 0

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        trips++
        val payload = result.data?.getStringExtra(ResultProviderActivity.EXTRA_PAYLOAD) ?: "none"
        val outcome = if (result.resultCode == Activity.RESULT_OK) "RESULT_OK" else "RESULT_CANCELED"
        setStatus("Round trips completed: $trips")
        setNote("Last result: $outcome — payload: $payload")
    }

    override val faultDescription =
        "EXERCISE — starts another Activity and comes back, to drive back navigation"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStatus("Round trips completed: $trips")
        setNote("Nothing started yet.")
        primaryButton("Start ResultProviderActivity") {
            launcher.launch(Intent(this, ResultProviderActivity::class.java))
        }
    }
}
