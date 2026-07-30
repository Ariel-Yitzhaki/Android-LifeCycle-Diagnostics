package com.ariel.lifecycle.sampleviews.screens

import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ariel.lifecycle.sampleviews.core.HeavyRows
import com.ariel.lifecycle.sampleviews.databinding.ActivityListBinding
import com.ariel.lifecycle.sampleviews.ui.RowViewHolder

/**
 * FAULT: every `onBindViewHolder` burns [HeavyRows.COST_MS] on the main thread, with no cache.
 *
 * A fling binds rows faster than 12 ms apiece allows, so frames are dropped for as long as the
 * list is moving — every time, in both directions.
 */
class JankListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        title = javaClass.simpleName

        binding.screenName.text = javaClass.simpleName
        binding.screenFault.text =
            "FAULT — ${HeavyRows.COST_MS}ms of blocking work per row, inside onBindViewHolder()"
        binding.screenStatus.text =
            "${HeavyRows.ROW_COUNT} rows, recomputed on every bind. Scroll to drop frames."

        binding.list.layoutManager = LinearLayoutManager(this)
        binding.list.adapter = JankAdapter()
    }

    private class JankAdapter : RecyclerView.Adapter<RowViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = RowViewHolder.create(parent)

        override fun onBindViewHolder(holder: RowViewHolder, position: Int) {
            // Blocking, on the main thread, on every single bind — including re-binds while flinging.
            holder.bind(HeavyRows.compute(position))
        }

        override fun getItemCount() = HeavyRows.ROW_COUNT
    }
}
