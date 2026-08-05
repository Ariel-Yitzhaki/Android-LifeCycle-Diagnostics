package com.ariel.diagnostics.leaks

import java.lang.ref.WeakReference

/**
 * One destroyed Activity, Fragment or Fragment view that the library is waiting to check on.
 *
 * Every field is either a plain value or a WeakReference, so [LeakWatcher] can keep these without
 * becoming the leak it is looking for.
 */
class WatchedComponent(

    /** Key [LeakWatcher] files this component under. */
    val id: Long,

    /** Simple class name of the screen this belongs to, for example "ActivityLeakActivity". */
    val screenName: String,

    /**
     * What this component is. Findings are grouped by screen name plus kind, because a Fragment and
     * its view share a class name but are two separate things to watch.
     *
     * For a Fragment view this is only the answer for the case where the fragment went away with
     * it. See [ownerReference].
     */
    val kind: WatchedKind,

    /** The only link back to the component. */
    val reference: WeakReference<Any>,

    /**
     * The Fragment that owned [reference] when this is a Fragment view, and null for everything
     * else. Never used for anything but asking whether it is still there, which is what chooses
     * between the two Fragment view kinds.
     *
     * Weak like the reference above: watching a view must not be the thing that keeps its fragment
     * alive.
     */
    val ownerReference: WeakReference<Any>?,
)
