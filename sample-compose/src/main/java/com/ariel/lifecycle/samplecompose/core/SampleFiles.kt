package com.ariel.lifecycle.samplecompose.core

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
