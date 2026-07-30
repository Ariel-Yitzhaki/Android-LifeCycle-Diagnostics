package com.ariel.diagnostics.lifecycle

/**
 * Remembers which screen classes have already produced a measurement in this process, so the very
 * first measurement for a class can be marked as such — that one is usually the slowest, because it
 * pays for class loading and for caches that are still cold.
 *
 * One instance is shared by the Activity and the Fragment callbacks so that a class counts as seen
 * no matter which of the two saw it first.
 */
class SeenScreens {

    // Class names only, never Activity or Fragment objects. This set lives as long as the process
    // does, so putting a screen object in it would keep that screen and its entire view tree in
    // memory for the rest of the session.
    //
    // A plain HashSet is safe without locking because every lifecycle callback that reaches this
    // class runs on the main thread.
    private val seen = HashSet<String>()

    /**
     * Returns true the first time it is called with a given name and false every time after.
     *
     * The first call for a class is almost always its onCreate, so in practice only the onCreate
     * line ends up marked. If I later want "first time in *this particular* callback" instead, put
     * screenName + "." + callbackName into the set rather than screenName alone.
     */
    fun isFirstTime(screenName: String): Boolean {
        // HashSet.add returns true only when the name was not in the set already, so this both
        // answers the question and records the answer in a single step.
        return seen.add(screenName)
    }
}
