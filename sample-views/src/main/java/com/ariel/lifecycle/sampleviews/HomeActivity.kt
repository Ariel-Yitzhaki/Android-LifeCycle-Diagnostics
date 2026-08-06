package com.ariel.lifecycle.sampleviews

import android.app.Application
import android.os.Bundle
import android.os.Process
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.ariel.lifecycle.sampleviews.databinding.ActivityHomeBinding
import com.ariel.lifecycle.sampleviews.ui.HomeAdapter
import com.ariel.lifecycle.sampleviews.ui.HomeRows

/**
 * Lists every screen in the app, grouped by the library feature it exercises, so any of them can be
 * reached, backed out of, and rotated.
 *
 * Each group carries the explanation of what that feature watches for, and each screen carries both
 * what it plants and what the library should print because of it.
 *
 * Plants nothing, and works to keep it that way: the rows are built once on a background thread at
 * app start, and a RecyclerView inflates only what is on screen. Building the whole list by hand in
 * onCreate used to be enough for this screen to report itself as slow.
 */
class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        title = javaClass.simpleName

        binding.screenName.text = javaClass.simpleName
        binding.screenStatus.text = "sample-views · ${ScreenCatalog.screens.size} screens in " +
            "${ScreenCatalog.categories.size} groups · pid ${Process.myPid()} " +
            "(${Application.getProcessName()})"

        binding.list.layoutManager = LinearLayoutManager(this)
        binding.list.adapter = HomeAdapter(SampleViewsApplication.homeRows()) { screen ->
            startActivity(screen.createIntent(this))
        }
    }
}
