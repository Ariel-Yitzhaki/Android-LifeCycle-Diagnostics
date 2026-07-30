package com.ariel.lifecycle.sampleviews

import android.app.Application
import android.os.Bundle
import android.os.Process
import android.util.TypedValue
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import com.ariel.lifecycle.sampleviews.databinding.ActivityHomeBinding

/**
 * Lists every screen in the app so any of them can be reached, backed out of, and rotated.
 *
 * Plants nothing itself: the diagnostics library should have nothing to say about this screen.
 */
class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        title = javaClass.simpleName

        binding.screenName.text = javaClass.simpleName
        binding.screenStatus.text = "sample-views · ${ScreenCatalog.screens.size} screens · " +
            "pid ${Process.myPid()} (${Application.getProcessName()})"

        val width = LinearLayout.LayoutParams.MATCH_PARENT
        val height = LinearLayout.LayoutParams.WRAP_CONTENT

        ScreenCatalog.screens.forEach { screen ->
            val button = AppCompatButton(this).apply {
                text = screen.name
                isAllCaps = false
                layoutParams = LinearLayout.LayoutParams(width, height).apply { topMargin = dp(12) }
                setOnClickListener { startActivity(screen.createIntent(this@HomeActivity)) }
            }
            val caption = AppCompatTextView(this).apply {
                text = screen.fault
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                layoutParams = LinearLayout.LayoutParams(width, height).apply {
                    marginStart = dp(4)
                    topMargin = dp(2)
                }
            }
            binding.container.addView(button)
            binding.container.addView(caption)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
