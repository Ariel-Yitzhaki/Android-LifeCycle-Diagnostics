package com.ariel.diagnostics.leaks

import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference

/**
 * Takes a component that has just been destroyed, holds it only through a WeakReference, and a few
 * seconds later checks on a background thread whether anything in the app is still holding it.
 *
 * Nothing in this class may ever hold a component strongly: if it did, every screen it watched would
 * be retained by the watcher itself and every check would report a leak.
 */
class LeakWatcher(private val tally: LeakTally) {

    // Every WeakReference this class creates is registered with this queue, so the runtime posts it
    // here once it has emptied it.
    private val clearedReferences = ReferenceQueue<Any>()

    // Components destroyed and waiting to be checked, filed under the id handed out in watch().
    // Entries are taken out again in checkComponent(), so this holds a handful at most.
    //
    // No locking: only ever touched from the background thread — see watch().
    private val watched = HashMap<Long, WatchedComponent>()

    private var handler: Handler? = null

    // Only read and written inside watch(), which only runs on the main thread.
    private var lastId = 0L

    // Creates the background thread this feature owns and the Handler that posts work onto it.
    // Called once by LeakDetection.install, before any lifecycle callbacks are registered.
    fun start() {
        // Background priority because checkComponent() asks for a garbage collection, which must
        // never compete with what the user is doing on screen.
        val thread = HandlerThread("leak-detection", Process.THREAD_PRIORITY_BACKGROUND)
        thread.start()
        // thread.looper blocks until the queue is ready, so the Handler is built after start().
        handler = Handler(thread.looper)
    }

    // Starts watching one destroyed component. Called on the main thread from an Activity's or
    // Fragment's onDestroy, or a Fragment view's onDestroyView. kind is "Activity", "Fragment" or
    // "Fragment view".
    //
    // This can never keep the component alive: from the moment the WeakReference is built, that
    // reference is the only link this library has to it.
    fun watch(component: Any, screenName: String, kind: String) {
        val currentHandler = handler
        if (currentHandler == null) {
            return
        }

        // A plain counter is enough because this only ever runs on the main thread.
        lastId++
        val id = lastId

        // Passing the queue as the second argument registers the reference with it. From here on
        // `component` must not be stored anywhere else, and neither message below may capture it.
        val reference = WeakReference(component, clearedReferences)
        val watchedComponent = WatchedComponent(id, screenName, kind, reference)

        // The map entry is added on the background thread so that one thread owns the map from end
        // to end. A message posted with no delay always arrives before one posted with a delay.
        currentHandler.post { watched[id] = watchedComponent }

        currentHandler.postDelayed({ checkComponent(id) }, LeakConstants.WATCH_DELAY_MS)
    }

    // Asks for a garbage collection and works out whether the component filed under the id is still
    // in memory, passing either answer to LeakTally — a finding is a share of the total, so the
    // collected components count as much as the retained ones.
    //
    // TODO: batch the checks that are due and run one collection for all of them instead of one per
    //  destroyed component.
    private fun checkComponent(id: Long) {
        // System.gc() is a request, not a command: the runtime may ignore it or collect only part of
        // the heap. That is why a "retained" answer can be a false alarm and why this feature waits
        // for a repeating pattern before reporting.
        System.gc()

        // References are emptied by the runtime shortly after the collection, on a thread of its
        // own. Sleeping is fine here: this is the library's own thread and nothing waits on it.
        Thread.sleep(LeakConstants.GC_SETTLE_MS)

        drainClearedReferences()

        val watchedComponent = watched.remove(id)
        if (watchedComponent == null) {
            return
        }

        // Compared with null on this one line and never put into a variable: a local holding the
        // component would be a strong reference, and the watcher would be doing the exact thing it
        // exists to detect.
        val retained = watchedComponent.reference.get() != null

        tally.recordResult(watchedComponent.screenName, watchedComponent.kind, retained)
    }

    // Empties the reference queue, which is what stops it growing for the whole session. The answer
    // itself comes from the reference in checkComponent(), not from this queue.
    private fun drainClearedReferences() {
        // poll() never blocks, so looping until null empties the queue and then stops.
        var cleared = clearedReferences.poll()
        while (cleared != null) {
            cleared = clearedReferences.poll()
        }
    }
}
