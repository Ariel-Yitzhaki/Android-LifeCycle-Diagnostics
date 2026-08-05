package com.ariel.diagnostics.leaks

import java.lang.ref.WeakReference

/**
 * One destroyed Activity, Fragment or Fragment view that the library is waiting to check on.
 *
 * Every field is either a plain value or a WeakReference, so [LeakWatcher] can keep these without
 * becoming the leak it is looking for.
 */
class WatchedComponent(

    /** Key [LeakWatcher] files this component under. Never appears in a log line. */
    val id: Long,

    /** Simple class name of the screen this belongs to, for example "ActivityLeakActivity". */
    val screenName: String,

    /**
     * "Activity", "Fragment" or "Fragment view". Findings are grouped by screen name plus kind,
     * because a Fragment and its view share a class name but are two separate things to watch.
     */
    val kind: String,

    /** The only link back to the component, and deliberately a weak one. */
    val reference: WeakReference<Any>,
)
