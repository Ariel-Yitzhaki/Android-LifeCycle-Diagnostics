package com.ariel.diagnostics.leaks

/**
 * The running tally for one screen class: how many of its instances were destroyed this session and
 * how many of those were still in memory when the watcher checked on them.
 *
 * Holds only names and numbers, so keeping one per screen class for the whole session cannot itself
 * retain a screen.
 */
class ScreenLeakRecord(

    /** Simple class name of the screen, for example "ActivityLeakActivity". */
    val screenName: String,

    /** What this tally counts, which also decides what the finding says to go and look at. */
    val kind: WatchedKind,
) {

    /** How many instances of this screen class have been destroyed and checked this session. */
    var destroyedCount = 0

    /** How many of those [destroyedCount] checks found the instance still in memory. */
    var retainedCount = 0

    /** True once a finding has been printed, so it is not repeated on every later destruction. */
    var reported = false
}
