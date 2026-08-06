package com.ariel.lifecycle.sampleviews.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.ariel.lifecycle.sampleviews.R
import com.ariel.lifecycle.sampleviews.ScreenCatalog
import com.ariel.lifecycle.sampleviews.core.RetainedViews
import com.ariel.lifecycle.sampleviews.databinding.FragmentSimpleBinding

/**
 * CONTROL: caches what a view is worth caching for — the data it was showing — and lets the view go.
 */
class ViewCaptureCleanFragment : Fragment() {

    private var binding: FragmentSimpleBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = FragmentSimpleBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // A String, not a View. It holds no Context, so it can live for the whole session.
        lastRenderedAt = "visit ${++visits}"

        val views = requireNotNull(binding)
        views.screenName.text = javaClass.simpleName
        views.screenFault.text =
            "CONTROL — caches the state the view was showing, never the view\n" +
                "Look for: ${ScreenCatalog.expectationFor(javaClass.simpleName)}"
        views.screenStatus.text =
            "Views held by RetainedViews: ${RetainedViews.size} — none of them this screen's"
        views.screenNote.text =
            "Cached instead: \"$lastRenderedAt\". Re-inflating a view is cheap; keeping one alive " +
                "for the life of the process is not."

        views.actionPrimary.isVisible = true
        views.actionPrimary.text = "Replace this fragment with a fresh copy"
        views.actionPrimary.setOnClickListener {
            parentFragmentManager.commit {
                replace(R.id.fragmentContainer, ViewCaptureCleanFragment())
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private companion object {
        var visits = 0
        var lastRenderedAt = "nothing yet"
    }
}
