package com.ariel.lifecycle.sampleviews.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import com.ariel.lifecycle.sampleviews.R
import com.ariel.lifecycle.sampleviews.ScreenCatalog
import com.ariel.lifecycle.sampleviews.core.GlobalListenerRegistry
import com.ariel.lifecycle.sampleviews.databinding.FragmentSimpleBinding

/**
 * FAULT: its ViewModel registers into a global singleton, never unregisters, and is given a
 * callback that captures this fragment.
 *
 * The view binding here is handled correctly on purpose. The only thing wrong on this screen is the
 * chain out of [LeakyRegistryViewModel]: registry → ViewModel → lambda → fragment.
 */
class ViewModelLeakFragment : Fragment() {

    private var binding: FragmentSimpleBinding? = null

    private val viewModel: LeakyRegistryViewModel by viewModels()

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
            "FAULT — ViewModel registers with a global singleton, never unregisters, and holds a " +
                "callback into this fragment\n" +
                "Look for: ${ScreenCatalog.expectationFor(javaClass.simpleName)}"
        views.screenNote.text =
            "Tap below four times, then wait a few seconds. Each tap replaces this fragment with " +
                "a fresh copy, with no back stack, so the old one is destroyed and its ViewModel " +
                "is cleared — and onCleared() undoes nothing. The registry never shrinks, and " +
                "every ViewModel in it is still holding the fragment that made it."

        // The leak. `render` is a method on this fragment, so the reference the ViewModel is being
        // handed is a reference to the fragment.
        viewModel.onUpdate = { render() }

        views.actionPrimary.isVisible = true
        views.actionPrimary.text = "Replace this fragment with a fresh copy"
        views.actionPrimary.setOnClickListener {
            parentFragmentManager.commit {
                replace(R.id.fragmentContainer, ViewModelLeakFragment())
            }
        }
        render()
    }

    private fun render() {
        binding?.screenStatus?.text =
            "This screen's ViewModel: @${Integer.toHexString(System.identityHashCode(viewModel))}\n" +
                "GlobalListenerRegistry holds ${GlobalListenerRegistry.size}: ${GlobalListenerRegistry.describe()}\n" +
                "Every one of those still points at the fragment that created it."
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}
