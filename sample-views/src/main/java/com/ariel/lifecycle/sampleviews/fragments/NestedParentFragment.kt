package com.ariel.lifecycle.sampleviews.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.ariel.lifecycle.sampleviews.R
import com.ariel.lifecycle.sampleviews.ScreenCatalog
import com.ariel.lifecycle.sampleviews.databinding.FragmentNestedBinding

/**
 * EXERCISE: hosts [NestedChildFragment] in a child FragmentManager of its own.
 *
 * A fragment inside a fragment is not reachable from the Activity's FragmentManager, so a watcher
 * that registers callbacks on that manager alone would never see the child at all — every timing
 * and every finding belonging to it would be missing. The library registers recursively, which is
 * what this screen is here to show: the child is measured exactly like any other screen.
 *
 * The child is the slow one. The parent does nothing but hold it.
 */
class NestedParentFragment : Fragment() {

    private var binding: FragmentNestedBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = FragmentNestedBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val views = requireNotNull(binding)

        views.screenName.text = javaClass.simpleName
        views.screenFault.text =
            "EXERCISE — a parent fragment hosting a child in its own child FragmentManager\n" +
                "Look for: ${ScreenCatalog.expectationFor(javaClass.simpleName)}"
        views.screenNote.text =
            "This fragment plants nothing. The child below spends " +
                "${NestedChildFragment.BUILD_MS}ms building its view, and is reported under its " +
                "own name rather than this one's."

        if (savedInstanceState == null) {
            // childFragmentManager, not parentFragmentManager: this is a manager the Activity
            // knows nothing about.
            childFragmentManager.commit { replace(R.id.childContainer, NestedChildFragment()) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}
