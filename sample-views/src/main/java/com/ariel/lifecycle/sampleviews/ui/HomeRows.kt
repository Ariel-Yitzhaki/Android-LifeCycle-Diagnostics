package com.ariel.lifecycle.sampleviews.ui

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import com.ariel.lifecycle.sampleviews.SampleScreen
import com.ariel.lifecycle.sampleviews.ScreenCatalog

/**
 * One line of the home list: either a block of explanation or a screen to open.
 *
 * The styled text is built on first use and kept, so a row costs nothing until it scrolls into
 * view and nothing again after that. The home screen is the one screen here that must not show up
 * in the library's findings, and forty spanned strings built up front in onCreate was enough to
 * put it over the 50 ms line on a cold start.
 */
sealed class HomeRow {

    /** The legend at the top and each group heading. */
    class Text(private val build: () -> CharSequence) : HomeRow() {
        val content: CharSequence by lazy { build() }
    }

    /** A button and the two-line caption under it. */
    class Screen(val screen: SampleScreen) : HomeRow() {
        val caption: CharSequence by lazy { HomeRows.captionFor(screen) }
    }
}

/** Turns the catalog into the flat list the home screen shows, with the styling baked in. */
object HomeRows {

    fun build(): List<HomeRow> {
        val rows = ArrayList<HomeRow>()
        rows += HomeRow.Text { ScreenCatalog.LEGEND }

        ScreenCatalog.categories.forEach { category ->
            rows += HomeRow.Text {
                categoryText(category.title, category.tags, category.explanation)
            }
            category.screens.forEach { screen -> rows += HomeRow.Screen(screen) }
        }
        return rows
    }

    /**
     * Realises the text of the rows that will be on screen when the list first appears, so even
     * those are not built during a lifecycle callback. Called from the sample's warm-up thread.
     */
    fun prepareFirstRows(rows: List<HomeRow>) {
        rows.take(ROWS_PREPARED).forEach { row ->
            when (row) {
                is HomeRow.Text -> row.content
                is HomeRow.Screen -> row.caption
            }
        }
    }

    // Group heading: title, the Logcat tag it reports under, and what it watches for.
    private fun categoryText(title: String, tags: String, explanation: String): CharSequence {
        val text = SpannableStringBuilder()

        val titleStart = text.length
        text.append(title).append('\n')
        text.setSpan(StyleSpan(Typeface.BOLD), titleStart, text.length, SPAN)
        text.setSpan(RelativeSizeSpan(1.25f), titleStart, text.length, SPAN)

        val tagStart = text.length
        text.append(tags).append('\n')
        text.setSpan(TypefaceSpan("monospace"), tagStart, text.length, SPAN)

        text.append(explanation)
        return text
    }

    // Screen caption: what it plants on the first line, what to look for on the second.
    internal fun captionFor(screen: SampleScreen): CharSequence {
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

    /** Roughly what fits on a phone screen before the user has to scroll. */
    private const val ROWS_PREPARED = 6

    private const val SPAN = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
}
