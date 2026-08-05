package com.ariel.diagnostics.lifecycle

/**
 * Remembers which screen classes have already produced a measurement in this process, so the first
 * measurement for a class can be marked as such. Shared by the Activity and Fragment callbacks.
 */
class SeenScreens {

    // Class names only: this set lives as long as the process, so a screen object here would keep
    // it and its whole view tree in memory for the rest of the session.
    private val seen = HashSet<String>()

    // Returns true the first time it is called with a given name and false every time after.
    fun isFirstTime(screenName: String): Boolean {
        return seen.add(screenName)
    }
}
