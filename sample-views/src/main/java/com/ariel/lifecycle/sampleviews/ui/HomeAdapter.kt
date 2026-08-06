package com.ariel.lifecycle.sampleviews.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ariel.lifecycle.sampleviews.SampleScreen
import com.ariel.lifecycle.sampleviews.databinding.ItemHomeScreenBinding
import com.ariel.lifecycle.sampleviews.databinding.ItemHomeTextBinding

/** Renders the home list. Two row types: a block of explanation, and a screen to open. */
class HomeAdapter(
    private val rows: List<HomeRow>,
    private val onOpen: (SampleScreen) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemViewType(position: Int): Int = when (rows[position]) {
        is HomeRow.Text -> TYPE_TEXT
        is HomeRow.Screen -> TYPE_SCREEN
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_TEXT) {
            TextHolder(ItemHomeTextBinding.inflate(inflater, parent, false))
        } else {
            ScreenHolder(ItemHomeScreenBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is HomeRow.Text -> (holder as TextHolder).binding.blockText.text = row.content
            is HomeRow.Screen -> {
                val binding = (holder as ScreenHolder).binding
                binding.openScreen.text = row.screen.name
                binding.openScreen.setOnClickListener { onOpen(row.screen) }
                // Built on first bind and kept by the row from then on. See HomeRow.
                binding.screenCaption.text = row.caption
            }
        }
    }

    override fun getItemCount() = rows.size

    private class TextHolder(val binding: ItemHomeTextBinding) :
        RecyclerView.ViewHolder(binding.root)

    private class ScreenHolder(val binding: ItemHomeScreenBinding) :
        RecyclerView.ViewHolder(binding.root)

    private companion object {
        const val TYPE_TEXT = 0
        const val TYPE_SCREEN = 1
    }
}
