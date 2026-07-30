package com.ariel.lifecycle.samplecompose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.ariel.lifecycle.samplecompose.nav.SampleNavHost
import com.ariel.lifecycle.samplecompose.ui.SampleTheme

/**
 * The single Activity hosting every route.
 *
 * [EXTRA_START_ROUTE] lets a screen relaunch the whole Activity and land back on itself, which is
 * how `relaunch-self` drives repeated destroy/create without leaving Compose navigation.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val startRoute = intent.getStringExtra(EXTRA_START_ROUTE)

        setContent {
            SampleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    SampleNavHost(startRoute = startRoute)
                }
            }
        }
    }

    companion object {
        const val EXTRA_START_ROUTE = "start_route"
        const val EXTRA_RELAUNCH_PASS = "relaunch_pass"
    }
}
