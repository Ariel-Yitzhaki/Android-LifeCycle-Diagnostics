package com.ariel.lifecycle.sampleviews.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.ariel.lifecycle.sampleviews.databinding.ActivitySimpleBinding

/**
 * Shared chrome for the single-purpose screens: shows the class name (so the fault is identifiable
 * from the device), a one-line description of what it plants, and up to two action buttons.
 *
 * Subclasses call `super.onCreate` first and then do their damage, so the layout is always up.
 */
abstract class SimpleScreenActivity : AppCompatActivity() {

    protected lateinit var binding: ActivitySimpleBinding
        private set

    /** One line describing what this screen plants, rendered under the title. */
    protected abstract val faultDescription: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySimpleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.screenName.text = javaClass.simpleName
        binding.screenFault.text = faultDescription
        title = javaClass.simpleName
    }

    protected fun setStatus(text: CharSequence) {
        binding.screenStatus.text = text
    }

    protected fun setNote(text: CharSequence) {
        binding.screenNote.text = text
    }

    protected fun primaryButton(label: CharSequence, onClick: () -> Unit) {
        binding.actionPrimary.apply {
            text = label
            isVisible = true
            setOnClickListener { onClick() }
        }
    }

    protected fun secondaryButton(label: CharSequence, onClick: () -> Unit) {
        binding.actionSecondary.apply {
            text = label
            isVisible = true
            setOnClickListener { onClick() }
        }
    }
}
