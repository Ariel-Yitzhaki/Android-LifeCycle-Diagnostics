package com.ariel.diagnostics.leaks

/**
 * What one watched component was, which decides both the tally its result is added to and what the
 * finding tells the reader to go and look at.
 *
 * The two Fragment view kinds are the reason this is a type rather than a name. A view destroyed
 * along with its fragment and a view destroyed while its fragment carried on are different faults:
 * the first is something outside still holding the view, the second is the fragment holding a view
 * it was told to let go of. They also happen at very different rates, because ordinary navigation
 * destroys a fragment and its view together while only the back stack leaves a fragment behind. One
 * tally holding both would let the common case, which is almost always clean, outvote the one worth
 * reporting.
 */
enum class WatchedKind {

    /** A whole Activity, watched from its onDestroy. */
    ACTIVITY,

    /** A whole Fragment, watched from its onDestroy. */
    FRAGMENT,

    /** A Fragment's view, destroyed along with the Fragment that owned it. */
    FRAGMENT_VIEW,

    /**
     * A Fragment's view, destroyed while the Fragment itself carried on, which is what happens to
     * every fragment put on the back stack. A retained one of these is a fragment still pointing at
     * the view its onDestroyView was supposed to release.
     *
     * A Fragment that has leaked is still alive too, so its view is filed here rather than under
     * [FRAGMENT_VIEW]. Nothing is lost by that: such a fragment gets a finding of its own under
     * [FRAGMENT], which is where the real cause is.
     */
    VIEW_OF_LIVE_FRAGMENT,
}
