package com.ariel.lifecycle.sampleviews.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ariel.lifecycle.sampleviews.databinding.ItemRowBinding
import kotlinx.coroutines.Job

/** One row of the jank screens. [job] is only used by the clean screen's async binding. */
class RowViewHolder(private val binding: ItemRowBinding) : RecyclerView.ViewHolder(binding.root) {

    var job: Job? = null

    fun bind(text: String) {
        binding.rowText.text = text
    }

    companion object {
        fun create(parent: ViewGroup): RowViewHolder = RowViewHolder(
            ItemRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }
}
