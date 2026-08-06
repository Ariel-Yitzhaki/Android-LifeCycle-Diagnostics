package com.ariel.lifecycle.sampleviews.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.ariel.lifecycle.sampleviews.databinding.FragmentSimpleBinding

/**
 * Plants nothing. Its job is to replace the fragment under it while keeping that fragment alive on
 * the back stack — which is exactly the situation where a retained view binding becomes a leak.
 */
class CoverFragment : Fragment() {

    private var binding: FragmentSimpleBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = FragmentSimpleBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val views = requireNotNull(binding)
        views.screenName.text = javaClass.simpleName
        views.screenFault.text = "No fault — a plain fragment placed on top of another"
        views.screenStatus.text =
            "The fragment underneath is on the back stack: its view has been destroyed, but the " +
                "fragment itself is still alive."
        views.screenNote.text =
            "Press Back to return to it. That round trip is one destroyed view — the library " +
                "needs three before it will report a pattern."
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}
