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
 * FAULT: hands its root view to a process-lifetime cache.
 *
 * This is the other way round from [FragmentViewLeakFragment]. There, the fragment survived and
 * held on to a view it should have released. Here the fragment goes away exactly as it should, and
 * something outside keeps the view instead — so the library reports a Fragment view whose fragment
 * is gone, which is a different fault with a different place to go and look.
 *
 * Caching a view to save inflating it again is the usual excuse. It never pays: every View holds
 * the Context it was inflated with, so a cached view is a cached Activity.
 */
class ViewCaptureFragment : Fragment() {

    private var binding: FragmentSimpleBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = FragmentSimpleBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // The whole fault. Nothing here points at the fragment, which is what makes this the
        // "fragment gone, view still here" case rather than the back-stack one.
        RetainedViews.keep(view)

        val views = requireNotNull(binding)
        views.screenName.text = javaClass.simpleName
        views.screenFault.text =
            "FAULT — puts its root view into a process-lifetime cache\n" +
                "Look for: ${ScreenCatalog.expectationFor(javaClass.simpleName)}"
        views.screenStatus.text =
            "Views held by RetainedViews: ${RetainedViews.size} (${RetainedViews.describe()})"
        views.screenNote.text =
            "Tap below four times. Each tap replaces this fragment with a fresh copy of itself, " +
                "with no back stack, so the old fragment and its view are both destroyed — and " +
                "only the view survives. Each cached view also holds the Activity it was " +
                "inflated with, so expect a finding about FragmentHostActivity too."

        views.actionPrimary.isVisible = true
        views.actionPrimary.text = "Replace this fragment with a fresh copy"
        views.actionPrimary.setOnClickListener {
            // No addToBackStack: the fragment being replaced is destroyed rather than kept, which
            // is what makes this the "fragment gone, view retained" case every time. Leaving the
            // Activity would do it too, but then the Activity itself is on its way out and
            // whether the fragment outlives its view stops being predictable.
            parentFragmentManager.commit { replace(R.id.fragmentContainer, ViewCaptureFragment()) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Correct, and not enough: this fragment is no longer the one holding the view.
        binding = null
    }
}
