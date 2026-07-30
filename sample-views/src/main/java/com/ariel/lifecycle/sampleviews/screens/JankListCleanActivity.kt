package com.ariel.lifecycle.sampleviews.screens

import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ariel.lifecycle.sampleviews.core.HeavyRows
import com.ariel.lifecycle.sampleviews.databinding.ActivityListBinding
import com.ariel.lifecycle.sampleviews.ui.RowViewHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * CONTROL: identical rows and identical per-row cost, computed off the main thread and memoised.
 *
 * Binding is a placeholder plus a coroutine, so no bind ever exceeds a frame and scrolling stays
 * smooth in both directions.
 */
class JankListCleanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        title = javaClass.simpleName

        binding.screenName.text = javaClass.simpleName
        binding.screenFault.text =
            "CONTROL — same ${HeavyRows.COST_MS}ms per row, computed on Dispatchers.Default and cached"
        binding.screenStatus.text =
            "${HeavyRows.ROW_COUNT} rows. Rows fill in as they resolve; scrolling never blocks."

        binding.list.layoutManager = LinearLayoutManager(this)
        binding.list.adapter = CleanAdapter(lifecycleScope)
    }

    private class CleanAdapter(private val scope: CoroutineScope) : RecyclerView.Adapter<RowViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = RowViewHolder.create(parent)

        override fun onBindViewHolder(holder: RowViewHolder, position: Int) {
            holder.job?.cancel()
            holder.job = null

            val ready = HeavyRows.cached(position)
            if (ready != null) {
                holder.bind(ready)
                return
            }

            holder.bind("Row $position — computing…")
            holder.job = scope.launch {
                val text = withContext(Dispatchers.Default) { HeavyRows.computeAndCache(position) }
                if (holder.bindingAdapterPosition == position) holder.bind(text)
            }
        }

        override fun onViewRecycled(holder: RowViewHolder) {
            holder.job?.cancel()
            holder.job = null
        }

        override fun getItemCount() = HeavyRows.ROW_COUNT
    }
}
