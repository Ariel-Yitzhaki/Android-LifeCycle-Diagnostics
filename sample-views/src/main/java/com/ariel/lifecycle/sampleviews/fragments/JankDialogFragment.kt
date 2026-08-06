package com.ariel.lifecycle.sampleviews.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ariel.lifecycle.sampleviews.core.HeavyRows
import com.ariel.lifecycle.sampleviews.databinding.DialogListBinding
import com.ariel.lifecycle.sampleviews.ui.RowViewHolder

/**
 * FAULT: the jank list again, inside a dialog.
 *
 * A `DialogFragment` draws into a window of its own, on top of the Activity's. Frames drawn here
 * are not the Activity window's frames, so a frame counter watching only Activities would count
 * none of this — every dropped frame in a dialog would be invisible. The library gives a
 * DialogFragment a counter of its own, and this screen is what proves it.
 */
class JankDialogFragment : DialogFragment() {

    private var binding: DialogListBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = DialogListBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val views = requireNotNull(binding)

        views.screenName.text = javaClass.simpleName
        views.screenFault.text =
            "FAULT — ${HeavyRows.COST_MS}ms per row, in a window of the dialog's own. Fling it."

        views.list.layoutManager = LinearLayoutManager(requireContext())
        views.list.adapter = DialogJankAdapter()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private class DialogJankAdapter : RecyclerView.Adapter<RowViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = RowViewHolder.create(parent)

        override fun onBindViewHolder(holder: RowViewHolder, position: Int) {
            holder.bind(HeavyRows.compute(position))
        }

        override fun getItemCount() = HeavyRows.ROW_COUNT
    }

    companion object {
        const val TAG = "JankDialogFragment"
    }
}
