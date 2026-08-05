package com.ariel.diagnostics.blocking

/**
 * Keeps the name of the screen in front of the user right now, so a finding from any of the three
 * detectors can say where it happened.
 *
 * A screen is not always an Activity. An app built as one Activity hosting fragments would have
 * every finding in the session attributed to that one Activity, which says nothing about which part
 * of the app was slow, so the fragments resumed on top of it are tracked too and the last one to
 * arrive names the screen.
 *
 * That last one is the outermost of a nested pair: a child fragment's onResume runs inside its
 * parent's, so the child's callback arrives first and the parent's overwrites it. A fragment that
 * hosts a map or a pager is therefore named rather than the library fragment inside it, which is
 * also the name the user would give the screen.
 */
class ForegroundScreenTracker {

    // A name and never an Activity object: this field lives as long as the process, so an Activity
    // here would leak the screen.
    //
    // @Volatile because it is written on the main thread and read from SlowMessageWatchdog's
    // background thread, which would otherwise sit on a stale value.
    @Volatile
    private var activityName: String? = null

    // Names of the fragments that have been resumed and not yet paused, oldest first. Names again
    // rather than Fragments, for the same reason as above.
    //
    // A plain list is enough because both functions that touch it are fragment lifecycle callbacks
    // on the main thread. What another thread reads is the field below, never this.
    private val resumedFragmentNames = ArrayList<String>()

    // The last name in the list above, kept as a field of its own so that a reading thread gets it
    // in a single step. Reading the list would take two, a length and then an element, and the main
    // thread can empty it in between.
    //
    // @Volatile for the same reason as the Activity name above.
    @Volatile
    private var fragmentName: String? = null

    fun onActivityResumed(name: String) {
        activityName = name
    }

    // Called from onActivityPaused, which Android always runs before resuming the incoming
    // Activity, so this cannot wipe out a newer name.
    //
    // The fragment names are deliberately left alone. A FragmentActivity pauses its own fragments
    // after this callback, so clearing them here would drop the name of the fragment whose onPause
    // is about to run, which is exactly the code a finding in that moment belongs to.
    fun onActivityPaused() {
        activityName = null
    }

    fun onFragmentResumed(name: String) {
        resumedFragmentNames.add(name)
        fragmentName = name
    }

    // Removes the first entry with this name, which is not always the instance that just paused
    // when two fragments of one class are resumed at once. Harmless: two instances of a class are
    // indistinguishable in the log anyway, and the list is only ever read for its last name.
    fun onFragmentPaused(name: String) {
        resumedFragmentNames.remove(name)
        fragmentName = resumedFragmentNames.lastOrNull()
    }

    /**
     * The screen a finding should be attributed to, or null when nothing is in the foreground,
     * which is the case while the app is in the background.
     */
    fun currentScreenName(): String? {
        // A resumed fragment is always in front of the Activity hosting it, so it wins.
        return fragmentName ?: activityName
    }

    // Builds the text a finding uses to say where it happened, for example "with HomeFeedFragment
    // (in MainActivity) in the foreground". Both names are given when both are known, because the
    // fragment says which part of the app it was and the Activity says which window it was in.
    fun describe(): String {
        // Read into locals so neither value can change between the checks and the uses below.
        val screenName = fragmentName
        val hostName = activityName

        if (screenName == null) {
            if (hostName == null) {
                return "with no screen in the foreground"
            }
            return "with $hostName in the foreground"
        }

        // The host is null for the moment between an Activity pausing and its fragments pausing.
        if (hostName == null) {
            return "with $screenName in the foreground"
        }
        return "with $screenName (in $hostName) in the foreground"
    }
}
