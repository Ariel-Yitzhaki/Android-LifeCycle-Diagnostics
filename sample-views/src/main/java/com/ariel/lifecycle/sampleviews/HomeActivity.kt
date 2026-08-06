package com.ariel.lifecycle.sampleviews

import android.app.Application
import android.graphics.Typeface
import android.os.Bundle
import android.os.Process
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.util.TypedValue
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import com.ariel.lifecycle.sampleviews.databinding.ActivityHomeBinding

/**
 * Lists every screen in the app, grouped by the library feature it exercises, so any of them can be
 * reached, backed out of, and rotated.
 *
 * Each group carries the explanation of what that feature watches for, and each screen carries both
 * what it plants and what the library should print because of it.
 *
 * Plants nothing itself. Its own onCreate is not free, though: it builds every button and caption
 * below by hand, which on a slow device is enough to cross the library's 50 ms line.
 */
class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        title = javaClass.simpleName

        binding.screenName.text = javaClass.simpleName
        binding.screenStatus.text = "sample-views · ${ScreenCatalog.screens.size} screens in " +
            "${ScreenCatalog.categories.size} groups · pid ${Process.myPid()} " +
            "(${Application.getProcessName()})"

        addParagraph(binding.container, ScreenCatalog.LEGEND, sizeSp = 12f, topMarginDp = 16)

        ScreenCatalog.categories.forEach { category ->
            addParagraph(binding.container, categoryText(category), sizeSp = 13f, topMarginDp = 28)

            category.screens.forEach { screen ->
                addButton(binding.container, screen)
                addParagraph(binding.container, screenText(screen), sizeSp = 12f, topMarginDp = 2)
            }
        }
    }

    // Group heading: title, the Logcat tag it reports under, and what it watches for.
    private fun categoryText(category: ScreenCategory): CharSequence {
        val text = SpannableStringBuilder()

        val titleStart = text.length
        text.append(category.title).append('\n')
        text.setSpan(StyleSpan(Typeface.BOLD), titleStart, text.length, SPAN)
        text.setSpan(RelativeSizeSpan(1.25f), titleStart, text.length, SPAN)

        val tagStart = text.length
        text.append(category.tags).append('\n')
        text.setSpan(TypefaceSpan("monospace"), tagStart, text.length, SPAN)

        text.append(category.explanation)
        return text
    }

    // Screen caption: what it plants on the first line, what to look for on the second.
    private fun screenText(screen: SampleScreen): CharSequence {
        val text = SpannableStringBuilder(screen.fault)

        // Bolds the FAULT / CONTROL / EXERCISE label, which is how the eye finds the pairs.
        val label = screen.fault.indexOf(' ')
        if (label > 0) {
            text.setSpan(StyleSpan(Typeface.BOLD), 0, label, SPAN)
        }

        text.append('\n')
        val lookStart = text.length
        text.append("Look for:")
        text.setSpan(StyleSpan(Typeface.BOLD), lookStart, text.length, SPAN)

        text.append(' ').append(screen.expect)
        return text
    }

    private fun addButton(container: LinearLayout, screen: SampleScreen) {
        val button = AppCompatButton(this).apply {
            text = screen.name
            isAllCaps = false
            layoutParams = rowParams(topMarginDp = 12)
            setOnClickListener { startActivity(screen.createIntent(this@HomeActivity)) }
        }
        container.addView(button)
    }

    private fun addParagraph(
        container: LinearLayout,
        content: CharSequence,
        sizeSp: Float,
        topMarginDp: Int,
    ) {
        val paragraph = AppCompatTextView(this).apply {
            text = content
            setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
            layoutParams = rowParams(topMarginDp).apply { marginStart = dp(4) }
        }
        container.addView(paragraph)
    }

    private fun rowParams(topMarginDp: Int) = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT,
    ).apply { topMargin = dp(topMarginDp) }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val SPAN = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
    }
}
