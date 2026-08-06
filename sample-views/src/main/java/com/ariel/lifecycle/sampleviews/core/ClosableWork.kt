package com.ariel.lifecycle.sampleviews.core

import android.content.Context
import java.io.FileInputStream
import java.util.Locale

/**
 * Opens the sample blob and, in one case, walks away without closing it.
 *
 * An unclosed stream is not noticed when it is dropped. `FileInputStream` registers itself with the
 * runtime's CloseGuard when it is opened, and the complaint is raised by the finalizer, whenever the
 * collector gets round to the abandoned object — which is what makes this a VM-level StrictMode
 * check rather than a thread-level one.
 *
 * Everything here runs on a background thread on purpose: opening a file is also a disk read, and
 * a thread policy belongs to the thread that set it, so keeping this off the main thread stops the
 * disk-read detector from answering a question this screen is not asking.
 */
object ClosableWork {

    private const val PROBE_BYTES = 64
    private const val FINALIZATION_ROUNDS = 3
    private const val FINALIZATION_PAUSE_MS = 150L

    /**
     * Opens a stream, reads a little, and abandons it still open.
     *
     * The open happens on a thread of its own, which is then joined and gone. That is not part of
     * the fault — it is what makes the demonstration reliable. The collector treats a running
     * thread's stack conservatively, so a reference that was in a local variable can still look
     * live long after the function holding it returned. Letting the thread die takes the whole
     * stack out of the picture, so the abandoned stream really is unreachable when the collection
     * below is asked for.
     */
    fun leakOneStream(context: Context): String {
        SampleFiles.ensureSeeded(context)

        var receipt = ""
        val opener = Thread({ receipt = openAndAbandon(context) }, "abandon-a-stream")
        opener.start()
        opener.join()

        return receipt
    }

    // Deliberately not `use {}` and deliberately not closed. This is how it happens in real code:
    // an early return, a thrown exception, or a stream handed to something that was never told to
    // close it.
    private fun openAndAbandon(context: Context): String {
        val stream = FileInputStream(SampleFiles.file(context))
        val read = stream.read(ByteArray(PROBE_BYTES))

        leaked++
        return String.format(
            Locale.US,
            "opened %s, read %d bytes, never closed (%d abandoned this session)",
            SampleFiles.file(context).name,
            read,
            leaked,
        )
    }

    /** The same open and the same read, closed by `use` however the block ends. */
    fun readOneStream(context: Context): String {
        SampleFiles.ensureSeeded(context)

        val read = FileInputStream(SampleFiles.file(context)).use { it.read(ByteArray(PROBE_BYTES)) }

        closed++
        return String.format(
            Locale.US,
            "opened %s, read %d bytes, closed by use{} (%d opened and closed this session)",
            SampleFiles.file(context).name,
            read,
            closed,
        )
    }

    /**
     * Asks for a collection so the finalizer gets round to whatever was abandoned above.
     *
     * Repeated with pauses because none of the three calls is a command: the collector decides when
     * to run, the finalizer daemon is a thread of its own, and CloseGuard only complains from
     * inside the finalizer. One round is often not enough.
     *
     * Only ever called from a background thread. This is the sample nudging the runtime so the
     * lesson does not depend on waiting; it is not something an app should do.
     */
    fun requestFinalization() {
        repeat(FINALIZATION_ROUNDS) {
            System.gc()
            System.runFinalization()
            Thread.sleep(FINALIZATION_PAUSE_MS)
        }
    }

    private var leaked = 0
    private var closed = 0
}
