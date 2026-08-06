package com.ariel.lifecycle.sampleviews.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.ariel.lifecycle.sampleviews.R
import com.ariel.lifecycle.sampleviews.ScreenCatalog
import com.ariel.lifecycle.sampleviews.databinding.ActivityFragmentHostBinding

/** Plain host for the fragment-based screens. Plants nothing itself. */
class FragmentHostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFragmentHostBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFragmentHostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val className = requireNotNull(intent.getStringExtra(EXTRA_FRAGMENT_CLASS)) {
            "FragmentHostActivity started without $EXTRA_FRAGMENT_CLASS"
        }
        val simpleName = className.substringAfterLast('.')
        val withoutContainer = intent.getBooleanExtra(EXTRA_NO_CONTAINER, false)
        title = simpleName

        binding.hostName.text = buildString {
            append("FragmentHostActivity → ")
            append(simpleName)
            if (withoutContainer) {
                // Nothing is drawn below, so the host has to say what is going on.
                append("\n\nAdded with add(fragment, tag) — no container, so the view it inflates ")
                append("is never attached and nothing appears below.")
                append("\n\nLook for: ")
                append(ScreenCatalog.expectationFor(simpleName))
            }
        }

        if (savedInstanceState == null) {
            val fragment = supportFragmentManager.fragmentFactory.instantiate(classLoader, className)
            supportFragmentManager.commit {
                if (withoutContainer) {
                    add(fragment, simpleName)
                } else {
                    replace(R.id.fragmentContainer, fragment)
                }
            }
        }
    }

    companion object {
        private const val EXTRA_FRAGMENT_CLASS = "fragment_class"
        private const val EXTRA_NO_CONTAINER = "no_container"

        fun intent(
            context: Context,
            fragment: Class<out Fragment>,
            withoutContainer: Boolean = false,
        ): Intent =
            Intent(context, FragmentHostActivity::class.java)
                .putExtra(EXTRA_FRAGMENT_CLASS, fragment.name)
                .putExtra(EXTRA_NO_CONTAINER, withoutContainer)
    }
}
