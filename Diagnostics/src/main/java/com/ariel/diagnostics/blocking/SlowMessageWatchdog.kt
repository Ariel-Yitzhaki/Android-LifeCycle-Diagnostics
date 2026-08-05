package com.ariel.diagnostics.blocking

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Process
import android.os.SystemClock

/**
 * Runs a countdown on a background thread for every message the main thread starts, and if a message
 * is still running when the countdown expires, captures what the main thread is doing and prints it.
 *
 * The countdown has to live on another thread: a stuck main thread cannot run the code that would
 * notice it is stuck.
 */
class SlowMessageWatchdog(
    private val logger: BlockingLogger,
    private val foregroundActivity: ForegroundActivityTracker,
) {

    // Kept so reportIfStillRunning() can ask the main thread for its stack from the background
    // thread. Holding a Thread is safe: it lives as long as the process and points at no screen.
    private val mainThread: Thread = Looper.getMainLooper().thread

    private var handler: Handler? = null

    // Both flags are written on the main thread and read on the background thread, so @Volatile
    // keeps the background thread from sitting on a stale value and reporting a finished message.
    @Volatile
    private var messageRunning = false

    @Volatile
    private var messageStartUptimeMillis = 0L

    // Built once and reused: it is posted and cancelled on every main-thread message, so a new
    // object each time would be thousands of allocations a minute.
    private val timeoutRunnable = Runnable { reportIfStillRunning() }

    // Creates the background thread this detector owns and the Handler that posts work onto it.
    // Called once by MainThreadBlocking.install, before the printer is attached to the Looper.
    fun start() {
        // Background priority: the detector must not become the thing it is looking for.
        val thread = HandlerThread("main-thread-watchdog", Process.THREAD_PRIORITY_BACKGROUND)
        thread.start()
        // thread.looper blocks until the queue is ready, so the Handler is built after start().
        handler = Handler(thread.looper)
    }

    // Starts the countdown for one main-thread message. Main-thread messages never overlap, so there
    // is only ever one countdown in flight and one start time to keep.
    fun onMessageStarted() {
        val currentHandler = handler
        if (currentHandler == null) {
            return
        }

        // uptimeMillis() stops while the device is in deep sleep and is the clock postDelayed()
        // counts in, so the two numbers always agree.
        messageStartUptimeMillis = SystemClock.uptimeMillis()
        // Written after the start time, so the background thread can never see the flag turn true
        // while the time next to it still belongs to the previous message.
        messageRunning = true

        currentHandler.postDelayed(timeoutRunnable, BlockingConstants.SLOW_MESSAGE_THRESHOLD_MS)
    }

    // Cancels the countdown because the message finished in time. This is the common case by far:
    // nearly every countdown started above is thrown away here having never run.
    fun onMessageFinished() {
        val currentHandler = handler
        if (currentHandler == null) {
            return
        }

        // Cleared before the cancel below, so a countdown that has already begun running on the
        // other thread still sees that the message is over and says nothing.
        messageRunning = false

        // removeCallbacks() cannot stop the Runnable once it has started, which leaves a small gap
        // where a message finishing in that same instant is still reported.
        //
        // TODO: give each message a number and have the countdown check it is still reporting on the
        //  message it was started for.
        currentHandler.removeCallbacks(timeoutRunnable)
    }

    // Prints a finding if the message is still running. Called by the Handler on the background
    // thread, once per message, so a message that blocks for ten seconds produces one line.
    private fun reportIfStillRunning() {
        if (!messageRunning) {
            return
        }

        // Read into a local first: the main thread could move on to the next message at any point
        // below, and a duration worked out from two messages' clocks would be nonsense.
        val startedAt = messageStartUptimeMillis
        val runningForMillis = SystemClock.uptimeMillis() - startedAt

        // Asking another thread for its stack briefly pauses it — another reason this library is
        // debug-only.
        val frames = mainThread.stackTrace

        // The stack is a photograph taken when the countdown expired, not a recording of the whole
        // message, so the slow part may already be gone by the time it is taken.
        logger.report(
            "the main thread has been busy with one message for $runningForMillis ms " +
                "(over the ${BlockingConstants.SLOW_MESSAGE_THRESHOLD_MS} ms limit) " +
                "${foregroundActivity.describe()} — main thread was in: ${describeStack(frames)} " +
                "(snapshot taken at the ${BlockingConstants.SLOW_MESSAGE_THRESHOLD_MS} ms mark, so " +
                "it can show code that ran after the slow part)",
        )
    }

    // Turns the captured stack into one line of text, innermost function first. Only the top few
    // frames are used; see BlockingConstants.STACK_FRAMES_LOGGED.
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
            // A StackTraceElement prints as "com.example.Thing.doWork(Thing.kt:42)", which Android
            // Studio turns into a clickable link in Logcat.
            line.append(frames[index].toString())
        }
        if (frames.size > limit) {
            line.append(" <- ")
            line.append(frames.size - limit)
            line.append(" more frames")
        }
        return line.toString()
    }
}
