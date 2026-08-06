package com.ariel.lifecycle.sampleviews.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.ariel.lifecycle.sampleviews.core.BusyWork
import com.ariel.lifecycle.sampleviews.databinding.FragmentSimpleBinding

/**
 * The child half of [NestedParentFragment]: slow to build, so there is something to measure.
 *
 * Nothing here knows it is nested. That is the point — it is an ordinary fragment, and the library
 * should report it as one.
 */
class NestedChildFragment : Fragment() {

    private var binding: FragmentSimpleBinding? = null

    private var receipt = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        receipt = BusyWork.spinAndDescribe(BUILD_MS)
        return FragmentSimpleBinding.inflate(inflater, container, false).also { binding = it }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val views = requireNotNull(binding)
        views.screenName.text = javaClass.simpleName
        views.screenFault.text = "The nested child — spends ${BUILD_MS}ms building its view"
        views.screenStatus.text =
            "Reported under NestedChildFragment, not NestedParentFragment, and not FragmentHostActivity."
        views.screenNote.text = receipt
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {
        const val BUILD_MS = 220L
    }
}
