package com.ariel.lifecycle.sampleviews.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.ariel.lifecycle.sampleviews.ScreenCatalog
import com.ariel.lifecycle.sampleviews.core.BusyWork
import com.ariel.lifecycle.sampleviews.databinding.FragmentSimpleBinding

/**
 * FAULT: builds its view slowly, with the cost split across `onCreateView` and `onViewCreated`.
 *
 * The split is the point. Neither callback is timed on its own — the library measures from the
 * fragment's onCreate to its view existing and reports the pair together, because in a real screen
 * the layout inflates in one and the views are found, wired up and given adapters in the other.
 */
class SlowViewBuildFragment : Fragment() {

    private var binding: FragmentSimpleBinding? = null

    private var inflateReceipt = ""
    private var wireUpReceipt = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Stands in for a layout too deep to inflate quickly, or a view built in code.
        inflateReceipt = BusyWork.spinAndDescribe(INFLATE_MS)

        val created = FragmentSimpleBinding.inflate(inflater, container, false)
        binding = created
        return created.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Stands in for adapters, listeners and the first bind of already-loaded data.
        wireUpReceipt = BusyWork.spinAndDescribe(WIRE_UP_MS)

        val views = requireNotNull(binding)
        views.screenName.text = javaClass.simpleName
        views.screenFault.text =
            "FAULT — spends ${INFLATE_MS}ms in onCreateView() and ${WIRE_UP_MS}ms in " +
                "onViewCreated()\nLook for: ${ScreenCatalog.expectationFor(javaClass.simpleName)}"
        views.screenStatus.text = "Total view build cost: about ${INFLATE_MS + WIRE_UP_MS}ms"
        views.screenNote.text =
            "onCreateView: $inflateReceipt\n\nonViewCreated: $wireUpReceipt\n\n" +
                "The library reports one number covering both, because a fragment's view is not " +
                "finished until the second of them returns."
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {
        const val INFLATE_MS = 180L
        const val WIRE_UP_MS = 140L
    }
}
