package com.ariel.diagnostics.leaks

/**
 * The running tally for one screen class: how many of its instances were destroyed this session and
 * how many of those were still in memory when the watcher checked on them.
 *
 * One of these exists per screen class per session and it lives as long as the process does. It
 * holds two names and two numbers and never points at an Activity, a Fragment or a View, so keeping
 * it forever costs nothing and it cannot itself be the thing retaining a screen.
 */
class ScreenLeakRecord(

    /** Simple class name of the screen, for example "ActivityLeakActivity". */
    val screenName: String,

    /** "Activity", "Fragment" or "Fragment view" — which of the three this tally counts. */
    val kind: String,
) {

    /** How many instances of this screen class have been destroyed and checked this session. */
    var destroyedCount = 0

    /** How many of those [destroyedCount] checks found the instance still in memory. */
    var retainedCount = 0

    /**
     * True once a finding has been printed for this screen class, so the same finding is not
     * repeated on every later destruction.
     */
    var reported = false

    /**
     * Builds the name this screen is printed under, for example "ActivityLeakActivity (Activity)".
     *
     * Called by [LeakLogger] when it has a finding to print. It is built fresh each time rather
     * than stored, because it is only needed on the rare line that actually reports something.
     */
    fun label(): String {
        return "$screenName ($kind)"
    }
}
