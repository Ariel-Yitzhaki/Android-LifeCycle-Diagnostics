package com.ariel.lifecycle.sampleviews.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ariel.lifecycle.sampleviews.ScreenCatalog
import com.ariel.lifecycle.sampleviews.core.BusyWork
import com.ariel.lifecycle.sampleviews.databinding.FragmentSimpleBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * CONTROL: the same work, done after the view exists and off the main thread.
 *
 * The view is inflated and shown empty, which is what the user actually wants: something on screen
 * now, filled in a moment later.
 */
class SlowViewBuildCleanFragment : Fragment() {

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
        views.screenFault.text =
            "CONTROL — the same " +
                "${SlowViewBuildFragment.INFLATE_MS + SlowViewBuildFragment.WIRE_UP_MS}ms, moved " +
                "off the main thread and done after the view exists\n" +
                "Look for: ${ScreenCatalog.expectationFor(javaClass.simpleName)}"
        views.screenStatus.text = "View built without blocking"
        views.screenNote.text = "preparing…"

        // viewLifecycleOwner, not the fragment: this work belongs to the view, and a fragment on
        // the back stack should not still be finishing it.
        viewLifecycleOwner.lifecycleScope.launch {
            val receipt = withContext(Dispatchers.Default) {
                BusyWork.spinAndDescribe(
                    SlowViewBuildFragment.INFLATE_MS + SlowViewBuildFragment.WIRE_UP_MS,
                )
            }
            binding?.screenNote?.text = receipt
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}
