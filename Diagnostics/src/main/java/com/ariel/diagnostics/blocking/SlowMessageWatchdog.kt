package com.ariel.diagnostics.blocking

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Process
import android.os.SystemClock

/**
 * Runs a countdown on a background thread for every message the main thread starts, and if a message
 * is still running when the countdown expires, captures what the main thread is doing and prints it.
 */
class SlowMessageWatchdog(
    private val logger: BlockingLogger,
    private val foregroundActivity: ForegroundActivityTracker,
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

        // Cleared before the cancel below, so a countdown that has already begun running on the
        // other thread still sees that the message is over and says nothing.
        messageRunning = false

        // removeMessages() cannot stop a countdown the background thread has already begun, so the
        // one belonging to this message may be part way through reportIfStillRunning() right now.
        // That is what the sequence number there is for.
        currentHandler.removeMessages(MSG_CHECK_SLOW_MESSAGE)
    }

    // Runs on the background thread. `sequence` is the number of the message this countdown was
    // started for, carried on the Message that woke it up.
    private fun reportIfStillRunning(sequence: Int) {
        if (!messageRunning) {
            return
        }

        // Read into a local first: the main thread could move on to the next message at any point
        // below, and a duration worked out from two messages' clocks would be wrong.
        val startedAt = messageStartUptimeMillis
        val runningForMillis = SystemClock.uptimeMillis() - startedAt

        // Asking another thread for its stack briefly pauses it.
        val frames = mainThread.stackTrace

        // Everything above was read while the main thread was free to move on, so the finding only
        // holds if the same message is still running now. The counter never goes backwards, so
        // finding this message's number still on it here means it was there for the reads above
        // too.
        if (!messageRunning || messageSequence != sequence) {
            return
        }

        logger.report(
            "the main thread has been busy with one message for $runningForMillis ms " +
                "(over the ${BlockingConstants.SLOW_MESSAGE_THRESHOLD_MS} ms limit) " +
                "${foregroundActivity.describe()}. Main thread was in: ${describeStack(frames)} " +
                "(snapshot taken at the ${BlockingConstants.SLOW_MESSAGE_THRESHOLD_MS} ms mark, so " +
                "it can show code that ran after the slow part)",
        )
    }

    private fun describeStack(frames: Array<StackTraceElement>): String {
        if (frames.isEmpty()) {
            // getStackTrace() hands back an empty array for a thread that is not running.
            return "no stack available"
        }

        var limit = BlockingConstants.STACK_FRAMES_LOGGED
        if (frames.size < limit) {
            limit = frames.size
        }

        val line = StringBuilder()
        for (index in 0 until limit) {
            if (index > 0) {
                line.append(" <- ")
            }
            line.append(frames[index].toString())
        }
        if (frames.size > limit) {
            line.append(" <- ")
            line.append(frames.size - limit)
            line.append(" more frames")
        }
        return line.toString()
    }

    private companion object {
        const val MSG_CHECK_SLOW_MESSAGE = 1
    }
}
