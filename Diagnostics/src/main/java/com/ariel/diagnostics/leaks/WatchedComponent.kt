package com.ariel.diagnostics.leaks

import java.lang.ref.WeakReference

/**
 * One destroyed Activity, Fragment or Fragment view that the library is waiting to check on.
 *
 * Every field here is either a plain value or a WeakReference. Nothing in this class points at the
 * component strongly, which is what makes it safe for [LeakWatcher] to keep a list of these for
 * five seconds without becoming the leak it is looking for.
 */
class WatchedComponent(

    /**
     * A number that no other watched component in this process shares. It is only used as the key
     * [LeakWatcher] files this component under, so it never appears in any log line.
     */
    val id: Long,

    /** Simple class name of the screen this belongs to, for example "ActivityLeakActivity". */
    val screenName: String,

    /**
     * "Activity", "Fragment" or "Fragment view" — which of the three kinds this is. Findings are
     * grouped and printed by screen name plus kind, because a Fragment and that Fragment's view
     * share a class name but are two separate things to watch.
     */
    val kind: String,

    /**
     * The only link back to the component itself.
     *
     * A WeakReference is a reference the garbage collector is allowed to ignore. If a WeakReference
     * is the last thing pointing at an object, the object is collected anyway and the reference is
     * emptied — which is exactly the question this feature asks. [LeakWatcher] explains how it is
     * built and why nothing else may hold the component.
     */
    val reference: WeakReference<Any>,
)
