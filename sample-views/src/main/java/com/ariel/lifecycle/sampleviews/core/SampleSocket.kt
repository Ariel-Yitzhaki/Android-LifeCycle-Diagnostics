package com.ariel.lifecycle.sampleviews.core

import android.os.NetworkOnMainThreadException
import android.os.SystemClock
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.Locale

/**
 * A TCP connection to a socket this app opened on the loopback interface.
 *
 * Nothing leaves the device and no server has to be reachable: StrictMode's network check fires
 * inside the socket call, before any bytes move. Loopback keeps the screen deterministic, which a
 * real host would not — an emulator with no network would fail before the check ran.
 */
object SampleSocket {

    private const val CONNECT_TIMEOUT_MS = 500
    private const val BACKLOG = 1

    // Bound once and left open for the session. Port 0 asks the system for a free port. Binding is
    // itself a network operation, so this is only ever built from a background thread.
    @Volatile
    private var listener: ServerSocket? = null

    /** Called from the sample's warm-up thread, so no screen ever binds this on the main thread. */
    @Synchronized
    fun ensureListening() {
        if (listener == null) {
            listener = ServerSocket(0, BACKLOG)
        }
    }

    /** Connects, reads nothing, closes. Whichever thread calls this is the one StrictMode judges. */
    fun connectBlocking(): String {
        val started = SystemClock.elapsedRealtime()
        val port = listener?.localPort ?: UNREACHABLE_PORT

        val outcome = try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(LOOPBACK, port), CONNECT_TIMEOUT_MS)
                "connected"
            }
        } catch (e: IOException) {
            // The check has already fired by the time connect returns, so a refusal is fine.
            "refused (${e.javaClass.simpleName})"
        } catch (e: NetworkOnMainThreadException) {
            // Only reachable if something restored Android's own death-on-network penalty. See
            // SampleViewsApplication, which clears it so the violation is reported instead.
            "killed by ${e.javaClass.simpleName} before StrictMode could report it"
        }

        val elapsed = SystemClock.elapsedRealtime() - started
        return String.format(
            Locale.US,
            "TCP to %s:%d %s in %d ms on %s",
            LOOPBACK,
            port,
            outcome,
            elapsed,
            Thread.currentThread().name,
        )
    }

    private const val LOOPBACK = "127.0.0.1"

    /** Used only if the warm-up has not bound a listener yet. Nothing answers on it, which is fine. */
    private const val UNREACHABLE_PORT = 1
}
