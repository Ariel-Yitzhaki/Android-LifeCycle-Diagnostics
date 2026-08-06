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

/** CONTROL: same binding, same cover-and-return flow, released in `onDestroyView`. */
class FragmentViewLeakCleanFragment : Fragment() {

    private var binding: FragmentSimpleBinding? = null

    private var viewsCreated = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val created = FragmentSimpleBinding.inflate(inflater, container, false)
        binding = created
        viewsCreated++
        return created.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val views = requireNotNull(binding)
        views.screenName.text = javaClass.simpleName
        views.screenFault.text = "CONTROL — same binding, nulled in onDestroyView()\n" +
            "Look for: ${ScreenCatalog.expectationFor(javaClass.simpleName)}"
        views.screenStatus.text =
            "View trees this fragment instance has created: $viewsCreated\n" +
                "View trees it still references: 1"
        views.screenNote.text =
            "Cover this fragment and come back as often as you like. Every old view is released " +
                "the moment it is destroyed, so only the one on screen is ever referenced."

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
        binding = null
    }
}
