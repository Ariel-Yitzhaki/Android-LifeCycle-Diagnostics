package com.ariel.diagnostics.leaks

import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference

/**
 * The heart of the feature. Takes a component that has just been destroyed, holds it only through a
 * WeakReference, and a few seconds later checks on a background thread whether anything in the app
 * is still holding it.
 *
 * Two Java classes do the real work here and neither is Android-specific:
 *
 * A WeakReference is a reference the garbage collector is allowed to ignore. If the only thing left
 * pointing at an object is a WeakReference, the object is collected anyway and the reference is
 * emptied. That is exactly the question this feature asks — "is anything other than us still
 * holding this screen?" — so it is both the tool that answers it and the only safe way to hold on
 * to a screen we suspect of leaking.
 *
 * A ReferenceQueue is a mailbox the runtime drops references into once it has emptied them, so a
 * program can find out that a collection happened without polling every object it cares about.
 *
 * Nothing in this class ever holds a component strongly. If it did, every screen it watched would
 * be retained by the watcher itself and every single check would report a leak.
 */
class LeakWatcher(private val tally: LeakTally) {

    // The mailbox described above. Every WeakReference this class creates is registered with this
    // queue, which means the runtime posts the reference object here once it has emptied it.
    private val clearedReferences = ReferenceQueue<Any>()

    // The components that have been destroyed and are waiting to be checked, filed under the id
    // handed out in watch().
    //
    // The values hold their component only weakly, so nothing in this map can keep a screen alive.
    // Entries are taken out again in checkComponent(), so the map holds a handful of entries at
    // most and does not grow over a session.
    //
    // No locking is needed: this map is only ever touched from the background thread, never from
    // the main thread — see watch() for how that is arranged.
    private val watched = HashMap<Long, WatchedComponent>()

    // Posts work onto the background thread. Null until start() has run.
    private var handler: Handler? = null

    // The id given to the last watched component. Only ever read and written inside watch(), which
    // only ever runs on the main thread, so a plain counter needs no locking.
    private var lastId = 0L

    /**
     * Creates the one background thread this feature owns and the Handler that posts work onto it.
     *
     * Called once by [LeakDetection.install], from the app's Application.onCreate, before any
     * lifecycle callbacks are registered. The thread runs for the life of the process, the same way
     * the lifecycle callbacks do, so there is nothing to shut down afterwards.
     */
    fun start() {
        // A HandlerThread is a plain background thread with a message queue attached to it. The
        // queue is the whole reason for using one: it is what lets us hand it a piece of work to
        // run five seconds from now, instead of writing a thread that sleeps in a loop and keeps
        // its own list of what is due.
        //
        // Background priority because checkComponent() below asks for a garbage collection, which
        // is expensive, and none of this must ever compete with what the user is doing on screen.
        val thread = HandlerThread("leak-detection", Process.THREAD_PRIORITY_BACKGROUND)
        // Nothing runs on the thread until it is started, and its message queue is built as it
        // starts up.
        thread.start()
        // A Handler posts work onto one specific thread's message queue. Reading thread.looper
        // waits until that queue is ready, which is why the Handler is built after start() and not
        // before it.
        handler = Handler(thread.looper)
    }

    /**
     * Starts watching one destroyed component.
     *
     * Called on the main thread by [LeakActivityCallbacks] and [LeakFragmentCallbacks], from the
     * callback that says this component is finished with: an Activity's onDestroy, a Fragment's
     * onDestroy, or a Fragment view's onDestroyView. [component] is the object itself, [screenName]
     * is the screen class it belongs to and [kind] is "Activity", "Fragment" or "Fragment view".
     *
     * Calling this can never keep [component] alive. From the moment the WeakReference below is
     * built, that reference is the only link this library has to it.
     */
    fun watch(component: Any, screenName: String, kind: String) {
        // No thread to check on, so there is nothing this function can do. In practice this cannot
        // happen, because install() calls start() before it registers the callbacks that call here.
        val currentHandler = handler
        if (currentHandler == null) {
            return
        }

        // A number no other watched component shares, used as the map key below. A plain counter is
        // enough because this function only ever runs on the main thread, so two ids can never be
        // handed out at the same moment.
        lastId++
        val id = lastId

        // This is the line the whole feature rests on.
        //
        // The WeakReference is built here, on the main thread, in the callback where we still have
        // the component in hand. Passing the queue as the second argument is what registers it with
        // that queue: when the collector empties this reference it also posts it to the queue.
        //
        // From here on, `component` is not stored anywhere else — not in a field, not in a list,
        // and not captured by either of the two messages below. If any of those held it directly,
        // the watcher would keep every screen it watched alive and would report a leak on all of
        // them, including the ones it caused itself.
        val reference = WeakReference(component, clearedReferences)
        val watchedComponent = WatchedComponent(id, screenName, kind, reference)

        // Both messages below carry watchedComponent, which holds nothing but that weak reference
        // and three plain values, so nothing waiting in the message queue can retain the screen.
        //
        // The map entry is added on the background thread rather than here, so that one thread owns
        // the map from end to end and no locking is needed anywhere. A message posted with no delay
        // is always delivered before one posted with a delay, so this always arrives first.
        currentHandler.post { watched[id] = watchedComponent }

        // The check itself, roughly five seconds from now. postDelayed is what turns "later, on
        // another thread" into a single line.
        currentHandler.postDelayed({ checkComponent(id) }, LeakConstants.WATCH_DELAY_MS)
    }

    /**
     * Asks for a garbage collection and then works out whether the component filed under [id] is
     * still in memory, passing the answer to [LeakTally] either way.
     *
     * Called by the Handler on the background thread, about five seconds after [watch] was called
     * for that component, and never called from anywhere else. Both answers are passed on: the
     * tally needs the number of components that were collected normally as much as the number that
     * were not, because a finding is a share of the total and not a count on its own.
     *
     * One garbage collection is asked for per destroyed component. The smarter way is to hold the
     * checks that are due and run a single collection for the whole batch; with a handful of
     * screens a few seconds apart, that is not worth the extra code here.
     */
    private fun checkComponent(id: Long) {
        // System.gc() asks the runtime to collect garbage now. It is a request and not a command:
        // the runtime is free to ignore it entirely, or to collect only part of the heap. That is
        // the reason a "retained" answer below can be a false alarm — the component may be
        // perfectly collectable and simply not have been collected yet. It is also the reason this
        // feature never reports a single retention and waits for a repeating pattern instead.
        System.gc()

        // The collection itself happens above, but references are emptied and posted to their queue
        // by the runtime shortly afterwards, on a thread of its own. Without this short pause a
        // component that has just been collected can still look like it is being retained. Sleeping
        // is fine here: this is the library's own thread and nothing is waiting on it.
        Thread.sleep(LeakConstants.GC_SETTLE_MS)

        drainClearedReferences()

        // remove() reads the entry and takes it out of the map in one step, so each component is
        // checked exactly once and the map does not grow over a session.
        val watchedComponent = watched.remove(id)
        if (watchedComponent == null) {
            // Nothing filed under this id. It cannot normally happen, since every id checked here
            // was put into the map first; this is a guard against a later change that starts
            // removing entries somewhere else.
            return
        }

        // get() hands back the component itself while it is still in memory, and null once the
        // collector has taken it.
        //
        // This is the other line where holding on strongly matters. The result is compared with
        // null on this one line and never put into a variable of its own: a local holding the
        // component would be a strong reference, and for as long as it lived the watcher would be
        // doing the exact thing it exists to detect.
        val retained = watchedComponent.reference.get() != null

        tally.recordResult(watchedComponent.screenName, watchedComponent.kind, retained)
    }

    /**
     * Empties the reference queue.
     *
     * Called by [checkComponent] on the background thread, just before each check. Every reference
     * the collector has emptied since the last call is sitting on the queue, and taking them off is
     * what stops the queue growing for the whole session.
     *
     * The queue is not what answers the question here — [checkComponent] asks the reference itself
     * whether it still points at anything. The smarter way is a small subclass of WeakReference
     * that carries the id, so that the queue hands back the ids of the collected components
     * directly and nothing has to be looked up twice.
     */
    private fun drainClearedReferences() {
        // poll() takes one emptied reference off the queue, or returns null when there is nothing
        // on it. It never blocks, so looping until null empties the whole queue and then stops.
        var cleared = clearedReferences.poll()
        while (cleared != null) {
            cleared = clearedReferences.poll()
        }
    }
}
