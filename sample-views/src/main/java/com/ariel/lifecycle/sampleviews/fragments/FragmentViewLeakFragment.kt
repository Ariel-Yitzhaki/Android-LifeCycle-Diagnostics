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
import com.ariel.lifecycle.sampleviews.databinding.FragmentSimpleBinding

/**
 * FAULT: holds the view binding in a field and never nulls it in `onDestroyView`.
 *
 * A fragment routinely outlives its view — put another fragment on top of it and the view is torn
 * down while the fragment sits on the back stack. Everything this field still points at (the root
 * view, its children, their contexts) stays reachable for as long as the fragment does.
 */
class FragmentViewLeakFragment : Fragment() {

    private var binding: FragmentSimpleBinding? = null

    /** Not the bug — just makes the accumulation countable on screen. */
    private val retainedViewTrees = mutableListOf<FragmentSimpleBinding>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val created = FragmentSimpleBinding.inflate(inflater, container, false)
        binding = created
        retainedViewTrees += created
        return created.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val views = requireNotNull(binding)
        views.screenName.text = javaClass.simpleName
        views.screenFault.text =
            "FAULT — keeps the view binding in a field and never nulls it in onDestroyView()\n" +
                "Look for: ${ScreenCatalog.expectationFor(javaClass.simpleName)}"
        views.screenStatus.text =
            "View trees this fragment instance has created: ${retainedViewTrees.size}\n" +
                "View trees it still references: ${retainedViewTrees.size} (should be 1)"
        views.screenNote.text =
            "Tap below, then press Back, and repeat four times. Covering this fragment destroys " +
                "its view while the fragment itself survives on the back stack — and this " +
                "fragment is still holding the view it was told to let go of. The library needs " +
                "three of those before it will call it a pattern."

        views.actionPrimary.isVisible = true
        views.actionPrimary.text = "Cover this fragment (destroys its view)"
        views.actionPrimary.setOnClickListener {
            parentFragmentManager.commit {
                replace(R.id.fragmentContainer, CoverFragment())
                addToBackStack(null)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // The one-line fix — `binding = null` — is deliberately missing.
    }
}
