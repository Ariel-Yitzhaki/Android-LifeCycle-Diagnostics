package com.ariel.lifecycle.sampleviews.core

import android.content.Context
import android.os.SystemClock
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Locale

/**
 * A 4 MiB blob the app writes once on first launch, so the disk-read screens have something real to
 * read without any manual setup.
 *
 * [readBlocking] deliberately reads in tiny unbuffered chunks and digests the result: that is both
 * genuine file I/O (thousands of read syscalls) and genuine CPU, so it stays measurable on every
 * visit even once the page cache is warm.
 */
object SampleFiles {

    private const val FILE_NAME = "diagnostics-sample-blob.bin"
    private const val SIZE_BYTES = 4L * 1024 * 1024
    private const val READ_CHUNK = 512
    private const val WRITE_CHUNK = 64 * 1024

    /** How much [writeBlocking] writes. Smaller than the blob: an fsync is expensive on its own. */
    private const val COPY_BYTES = 2L * 1024 * 1024

    /** Keeps each copy on its own path, so no write is served from a file that already exists. */
    private var copyCounter = 0

    fun file(context: Context): File = File(context.filesDir, FILE_NAME)

    /** Idempotent. Called from a background thread at app start, and defensively before each read. */
    @Synchronized
    fun ensureSeeded(context: Context) {
        val target = file(context)
        if (target.exists() && target.length() == SIZE_BYTES) return

        val block = ByteArray(WRITE_CHUNK)
        var value = 7
        for (i in block.indices) {
            value = value * 31 + i
            block[i] = value.toByte()
        }
        FileOutputStream(target).use { out ->
            repeat((SIZE_BYTES / WRITE_CHUNK).toInt()) { out.write(block) }
        }
    }

    /**
     * Synchronous write of a copy of the blob. Whichever thread calls this pays for all of it, and
     * on the main thread it is what StrictMode reports as a disk write.
     *
     * A fresh file name every time, so the write is real rather than a no-op, and each copy is
     * deleted once its size has been read back.
     */
    fun writeBlocking(context: Context): String {
        val started = SystemClock.elapsedRealtime()
        val target = File(context.filesDir, "diagnostics-sample-copy-${copyCounter++}.bin")

        val block = ByteArray(WRITE_CHUNK)
        var written = 0L
        FileOutputStream(target).use { out ->
            repeat((COPY_BYTES / WRITE_CHUNK).toInt()) {
                out.write(block)
                written += WRITE_CHUNK
            }
            // Forces the bytes past the page cache, so the cost is a write and not a memcpy.
            out.fd.sync()
        }
        val onDisk = target.length()
        target.delete()

        val elapsed = SystemClock.elapsedRealtime() - started
        return String.format(
            Locale.US,
            "%,d KiB written in %,d chunks of %d KiB, fsync'd and deleted — %d ms on %s (%,d B on disk)",
            written / 1024,
            written / WRITE_CHUNK,
            WRITE_CHUNK / 1024,
            elapsed,
            Thread.currentThread().name,
            onDisk,
        )
    }

    /** Synchronous read of the whole blob. Whichever thread calls this pays for all of it. */
    fun readBlocking(context: Context): String {
        ensureSeeded(context)
        val started = SystemClock.elapsedRealtime()
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(READ_CHUNK)
        var total = 0L
        var reads = 0L
        FileInputStream(file(context)).use { input ->
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
                total += read
                reads++
            }
        }
        val elapsed = SystemClock.elapsedRealtime() - started
        val sha = digest.digest().take(6).joinToString("") { String.format(Locale.US, "%02x", it) }
        return String.format(
            Locale.US,
            "%,d KiB in %,d reads of %d B — %d ms on %s — sha256 %s…",
            total / 1024,
            reads,
            READ_CHUNK,
            elapsed,
            Thread.currentThread().name,
            sha,
        )
    }
}
