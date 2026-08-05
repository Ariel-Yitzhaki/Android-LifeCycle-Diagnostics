package com.ariel.diagnostics.leaks

/**
 * Keeps one ScreenLeakRecord per screen class and decides when its numbers are strong enough to be
 * worth printing. Nothing in this feature ever reports one retained component on its own.
 */
class LeakTally(private val logger: LeakLogger) {

    // Keys and values hold only strings and numbers, so this map can live for the whole session
    // without keeping a screen in memory.
    //
    // No locking: recordResult() is called from LeakWatcher's background thread and nowhere else.
    private val records = HashMap<String, ScreenLeakRecord>()

    // Adds the result of one check to the tally for its screen class, and prints a finding if that
    // class has now shown a pattern. retained is true when the component was still in memory.
    fun recordResult(screenName: String, kind: String, retained: Boolean) {
        // The kind is part of the key because a Fragment and its view share a class name.
        val key = "$screenName/$kind"

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

    // Prints a finding for the record once it has been destroyed enough times and at least half of
    // those were retained.
    private fun reportIfPattern(record: ScreenLeakRecord) {
        if (record.reported) {
            return
        }

        if (record.destroyedCount < LeakConstants.MIN_DESTROY_COUNT) {
            return
        }

        // Written as a multiplication so there is no division and no rounding.
        if (record.retainedCount * 2 < record.destroyedCount) {
            return
        }

        record.reported = true
        logger.report(record)
    }
}
