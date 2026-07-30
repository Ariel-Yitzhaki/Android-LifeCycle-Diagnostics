package com.ariel.diagnostics.leaks

/**
 * Every tunable number and string this feature uses, in one place. Changing how long the watcher
 * waits, or how sure it has to be before it says anything, should never mean reading the code that
 * does the detecting.
 */
object LeakConstants {

    /**
     * The single Logcat tag the whole feature prints under, so `adb logcat -s LeakDetection` shows
     * every finding and nothing else.
     *
     * Feature 1 and Feature 2 each have a tag of their own in the same way. This feature gets a
     * third one so leak findings can be filtered apart from timing lines and validation findings.
     */
    const val LOG_TAG = "LeakDetection"

    /**
     * How long to wait after a component is destroyed before checking whether it is still in
     * memory.
     *
     * Five seconds is a compromise. Checking straight away would report almost everything, because
     * the framework itself still holds a destroyed screen for a short while as it finishes tearing
     * it down and brings the next one in. Waiting much longer would mean finding a leak on a screen
     * the user left ages ago, which is harder to connect to what they were doing.
     */
    const val WATCH_DELAY_MS = 5_000L

    /**
     * How long to pause after asking for a garbage collection, in milliseconds.
     *
     * The runtime empties weak references on a thread of its own, which runs shortly after the
     * collection itself. Without this pause a component that has just been collected can still look
     * like it is being retained.
     */
    const val GC_SETTLE_MS = 100L

    /**
     * How many destructions of one screen class have to be seen before a finding can be printed at
     * all.
     *
     * Three is deliberately low but not one: a screen destroyed once or twice has not shown a
     * pattern yet, and a single retention on its own is usually noise.
     */
    const val MIN_DESTROY_COUNT = 3
}
