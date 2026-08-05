package com.ariel.diagnostics.blocking

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import com.ariel.diagnostics.StackSummary

/**
 * Runs a countdown on a background thread for every message the main thread starts, and if a message
 * is still running when the countdown expires, captures what the main thread is doing and prints it.
 *
 * A message that trips the countdown is reported twice: once at the threshold, with the stack, and
 * again when it finally ends, with the duration. The first line cannot carry a duration worth
 * printing, because it is written at a fixed point in time and so would always report roughly the
 * threshold no matter how long the message went on to run.
 */
class SlowMessageWatchdog(
    private val logger: BlockingLogger,
    private val foregroundScreen: ForegroundScreenTracker,
    private val busyTracker: MainThreadBusyTracker,
) {

    private val mainThread: Thread = Looper.getMainLooper().thread

    private var handler: Handler? = null

    // Written on the main thread and read on the background thread, so @Volatile keeps the
    // background thread from sitting on a stale value.
    @Volatile
    private var messageRunning = false

    @Volatile
    private var messageStartUptimeMillis = 0L

    // Numbers the main-thread messages so a countdown can tell whether it is still reporting on the
    // message it was started for. Only ever counts up, which is what lets reportIfStillRunning()
    // check it once at the end and know it held for every read before that. Written only on the
    // main thread, so the increment needs no atomic; wrapping past Int.MAX_VALUE is harmless
    // because it is only ever compared for equality.
    @Volatile
    private var messageSequence = 0

    // Sequence number of the last message a finding was printed for, so onMessageFinished knows
    // which messages have a first line to follow up on and leaves every other message silent.
    //
    // Written on the background thread and read on the main thread. Zero means nothing has been
    // reported yet, which no real message can be, since messageSequence is incremented before use.
    @Volatile
    private var reportedSequence = 0

    fun start() {
        val thread = HandlerThread("main-thread-watchdog", Process.THREAD_PRIORITY_BACKGROUND)
        thread.start()
        // thread.looper blocks until the queue is ready, so the Handler is built after start().
        //
        // A callback rather than a posted Runnable, because a Message can carry the sequence number
        // of the message its countdown belongs to.
        handler = Handler(thread.looper) { message ->
            if (message.what == MSG_CHECK_SLOW_MESSAGE) {
                reportIfStillRunning(message.arg1)
            }
            true
        }
    }

    // Main-thread messages never overlap, so there is only ever one countdown in flight and one
    // start time to keep.
    fun onMessageStarted() {
        val currentHandler = handler
        if (currentHandler == null) {
            return
        }

        // Counted up before the two fields below are touched, so a countdown that finds its own
        // number still on the counter knows neither of them has been rewritten under it.
        val sequence = ++messageSequence

        // uptimeMillis() is the clock sendMessageDelayed() counts in, so the two always agree.
        messageStartUptimeMillis = SystemClock.uptimeMillis()
        // Written after the start time, so the background thread can never see the flag turn true
        // while the time next to it still belongs to the previous message.
        messageRunning = true

        currentHandler.sendMessageDelayed(
            currentHandler.obtainMessage(MSG_CHECK_SLOW_MESSAGE, sequence, 0),
            BlockingConstants.SLOW_MESSAGE_THRESHOLD_MS,
        )
    }

    fun onMessageFinished() {
        val currentHandler = handler
        if (currentHandler == null) {
            return
        }

        // The Printer is attached from inside a main-thread message, so the first line it is ever
        // handed is that message's own finish, with no start to pair it with. Its start time is
        // still zero, and a duration measured from there would be the time since the device booted.
        if (messageSequence == 0) {
            return
        }

        // Cleared before the cancel below, so a countdown that has already begun running on the
        // other thread still sees that the message is over and says nothing.
        messageRunning = false

        // removeMessages() cannot stop a countdown the background thread has already begun, so the
        // one belonging to this message may be part way through reportIfStillRunning() right now.
        // That is what the sequence number there is for.
        currentHandler.removeMessages(MSG_CHECK_SLOW_MESSAGE)

        // Written only in onMessageStarted, on this same thread, so the next message cannot have
        // moved the start time before this runs.
        val totalMillis = SystemClock.uptimeMillis() - messageStartUptimeMillis

        // Every message is handed over, not only the ones that tripped the countdown: a screen held
        // up by two hundred short messages never trips it and would otherwise be reported nowhere.
        busyTracker.onMessageFinished(totalMillis)

        reportDurationIfReported(totalMillis)
    }

    // Runs on the main thread, straight after a message ends. Prints the second half of a finding:
    // how long the message ran in total, which is the number the first line could not give.
    private fun reportDurationIfReported(totalMillis: Long) {
        // The background thread may still be inside reportIfStillRunning() and not have set
        // reportedSequence yet, in which case this message loses its follow-up line. Harmless: the
        // first line has already been printed and carries the stack, which is the useful half.
        if (reportedSequence != messageSequence) {
            return
        }

        // Only ever reached for a message that already tripped the countdown, so this cannot add
        // logging to the ordinary path, which runs thousands of times a minute.
        logger.report(
            "that message finished after $totalMillis ms in total " +
                "${foregroundScreen.describe()}",
        )
    }

    // Runs on the background thread. `sequence` is the number of the message this countdown was
    // started for, carried on the Message that woke it up.
    private fun reportIfStillRunning(sequence: Int) {
        if (!messageRunning) {
            return
        }

        // Asking another thread for its stack briefly pauses it.
        val frames = mainThread.stackTrace

        // The stack above was read while the main thread was free to move on, so the finding only
        // holds if the same message is still running now. The counter never goes backwards, so
        // finding this message's number still on it here means it was there for the read above too.
        if (!messageRunning || messageSequence != sequence) {
            return
        }

        // Set before the line is printed, so the main thread cannot see the finding go out and then
        // find no follow-up owed for it.
        reportedSequence = sequence

        // No duration here on purpose. This runs at a fixed delay after the message started, so any
        // duration measured now would be the threshold plus scheduling jitter, and a 210 ms hiccup
        // would be indistinguishable from a four second freeze. reportDurationIfReported() prints
        // the real total once the message ends.
        logger.report(
            "the main thread has been stuck on one message for over " +
                "${BlockingConstants.SLOW_MESSAGE_THRESHOLD_MS} ms " +
                "${foregroundScreen.describe()}. Main thread was in: " +
                "${StackSummary.describe(frames, BlockingConstants.STACK_FRAMES_LOGGED)} " +
                "(snapshot taken at the ${BlockingConstants.SLOW_MESSAGE_THRESHOLD_MS} ms mark, so " +
                "it can show code that ran after the slow part)",
        )
    }

    private companion object {
        const val MSG_CHECK_SLOW_MESSAGE = 1
    }
}
