package com.ariel.diagnostics.blocking

/**
 * How much of the main thread one screen used in the first seconds after it came to the front.
 *
 * None of these fields is @Volatile, unlike the frame counts in ScreenJankRecord: main-thread
 * messages are timed by the Printer on the main thread itself, so one thread writes and reads all
 * of this.
 */
class ScreenBusyRecord(

    /** Simple class name of the Activity or Fragment this belongs to. */
    val screenName: String,

    /**
     * When the screen came to the front, on the uptime clock. In the same units as the message
     * durations added to [busyMillis], which is what lets the two be compared.
     */
    val startUptimeMillis: Long,
) {

    /** How many of the milliseconds since [startUptimeMillis] the main thread spent running work. */
    var busyMillis = 0L

    /**
     * How many messages that work was spread over. A screen held up by one long message is already
     * reported by the slow-message detector; one held up by two hundred short ones is not, and this
     * is what tells the two apart.
     */
    var messageCount = 0
}
