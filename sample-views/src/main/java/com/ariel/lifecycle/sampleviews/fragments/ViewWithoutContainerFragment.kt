package com.ariel.lifecycle.sampleviews.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.ariel.lifecycle.sampleviews.databinding.FragmentSimpleBinding

/**
 * EXERCISE: added with no container, and still inflates a view from `onCreateView`.
 *
 * `add(fragment, "tag")` is how a fragment is used as a headless worker — no UI, just a scope that
 * survives configuration changes. Such a fragment is expected to leave `onCreateView` alone. This
 * one does not: it inflates a full view tree that has nowhere to be attached.
 *
 * This is as close as app code can get to the fault the library's view counting looks for — a
 * fragment destroyed with a view that never got `onDestroyView`. It does not produce it, and it
 * cannot: `FragmentStateManager` dispatches `onFragmentViewCreated` and `onFragmentViewDestroyed`
 * from matched pairs on both of its code paths, the container-less one included. The check is
 * there for a host that gets that wrong, and androidx does not.
 *
 * So the screen is a control for that check rather than a fixture: it proves the counting survives
 * the strangest lifecycle an app can legitimately ask for.
 */
class ViewWithoutContainerFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // container is null, because this fragment was added without one. The view is built anyway.
        val created = FragmentSimpleBinding.inflate(inflater, container, false)
        created.screenName.text = javaClass.simpleName
        created.screenFault.text = "This view tree was built for a fragment that has nowhere to put it."
        return created.root
    }
}
