package com.ariel.lifecycle.sampleviews.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.ariel.lifecycle.sampleviews.ScreenCatalog
import com.ariel.lifecycle.sampleviews.core.GlobalListenerRegistry
import com.ariel.lifecycle.sampleviews.databinding.FragmentSimpleBinding

/**
 * FAULT: its ViewModel registers into a global singleton and never unregisters.
 *
 * The view binding here is handled correctly on purpose — the only thing wrong on this screen is
 * [LeakyRegistryViewModel].
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
            "FAULT — ViewModel registers with a global singleton and never unregisters in " +
                "onCleared()\nLook for: ${ScreenCatalog.expectationFor(javaClass.simpleName)}"
        views.screenNote.text =
            "Leave this screen and come back: onCleared() runs, but the registry never shrinks. " +
                "The count below is the only place this leak is visible — Logcat stays silent, " +
                "because a ViewModel that holds no Activity, Fragment or View is not something " +
                "the library watches."

        views.actionPrimary.isVisible = true
        views.actionPrimary.text = "Refresh registry count"
        views.actionPrimary.setOnClickListener { render() }
        render()
    }

    private fun render() {
        binding?.screenStatus?.text =
            "This screen's ViewModel: @${Integer.toHexString(System.identityHashCode(viewModel))}\n" +
                "GlobalListenerRegistry holds ${GlobalListenerRegistry.size}: ${GlobalListenerRegistry.describe()}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}
