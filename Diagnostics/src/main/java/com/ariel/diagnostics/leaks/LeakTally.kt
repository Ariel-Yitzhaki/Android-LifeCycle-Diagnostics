package com.ariel.diagnostics.leaks

/**
 * Keeps one [ScreenLeakRecord] per screen class and decides when the numbers in it are strong
 * enough to be worth printing.
 *
 * This is the class that turns a stream of single checks into a finding. Nothing in this feature
 * ever reports one retained component on its own.
 */
class LeakTally(private val logger: LeakLogger) {

    // One record per screen class, keyed by class name plus kind.
    //
    // The key is a String and the value holds nothing but Strings and numbers, so this map can live
    // for the whole session without keeping a single screen in memory. That matters here more than
    // anywhere else in the library: a map of leak findings that itself held the leaked screens
    // would be a joke.
    //
    // The number of distinct screen classes in an app is small and fixed, so the map cannot grow
    // without bound.
    //
    // No locking is needed: recordResult() below is called from LeakWatcher's background thread and
    // from nowhere else, so a single thread owns this map from end to end.
    private val records = HashMap<String, ScreenLeakRecord>()

    /**
     * Adds the result of one check to the tally for its screen class, and prints a finding if that
     * class has now shown a pattern.
     *
     * Called by [LeakWatcher] on its background thread, once per destroyed component, about five
     * seconds after that component was destroyed. [retained] is true when the component was still
     * in memory at that point and false when the garbage collector had already taken it.
     */
    fun recordResult(screenName: String, kind: String, retained: Boolean) {
        // The kind is part of the key and not just the name, because a Fragment and that Fragment's
        // view share a class name while being two separate things to watch. A slash is used rather
        // than the printed format so it is obvious this string is an internal key.
        val key = "$screenName/$kind"

        // First check ever seen for this screen class, so start a record for it.
        var record = records[key]
        if (record == null) {
            record = ScreenLeakRecord(screenName, kind)
            records[key] = record
        }

        record.destroyedCount++
        if (retained) {
            record.retainedCount++
        }

        reportIfPattern(record)
    }

    /**
     * Prints a finding for [record] if it now meets both conditions, and does nothing otherwise.
     *
     * Called by [recordResult] every time a record changes, which is the only moment either count
     * can cross a threshold.
     *
     * Both conditions exist for the same reason. A screen that is retained once, or once out of
     * many, is nearly always noise: the garbage collector may simply not have got round to it, the
     * framework may still be holding it for reasons of its own, or it may be one of the known
     * Android bugs where a system service keeps the last Activity alive. A screen that is retained
     * again and again, on a share of its destructions that does not fall as the user keeps visiting
     * it, is the app's own bug — every visit adds another copy that never goes away.
     */
    private fun reportIfPattern(record: ScreenLeakRecord) {
        if (record.reported) {
            // Reported once per screen class per session, so a leaking screen does not print a new
            // line on every single visit for the rest of the run. The counts carry on rising in the
            // background; if I later want a periodic reminder, the change is to report again every
            // few further retentions rather than never.
            return
        }

        if (record.destroyedCount < LeakConstants.MIN_DESTROY_COUNT) {
            // Too early to tell. Not enough of this screen's life has been watched yet.
            return
        }

        // "At least half were retained", written as a multiplication so there is no division and no
        // rounding to argue about: retained * 2 >= destroyed says the same thing as
        // retained >= destroyed / 2 and works in whole numbers.
        if (record.retainedCount * 2 < record.destroyedCount) {
            return
        }

        // Set before printing, so this record is marked as done whatever the logger does.
        record.reported = true
        logger.report(record)
    }
}
