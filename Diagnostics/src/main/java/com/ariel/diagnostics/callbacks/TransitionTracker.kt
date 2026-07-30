package com.ariel.diagnostics.callbacks

import java.util.WeakHashMap

/**
 * Check A. Keeps the current lifecycle state of every live Activity and Fragment instance, and
 * reports the four situations that mean something went wrong: a component that died without ever
 * reaching resumed, one whose onStart and onStop counts do not match, an Activity class that keeps
 * being recreated, and a Fragment whose view outlived its onDestroyView.
 *
 * It also reports any lifecycle step that does not follow the one before it, such as a callback
 * arriving for a component that has already been destroyed.
 *
 * Everything here is driven by [ActivityValidationCallbacks] and [FragmentValidationCallbacks];
 * this class never talks to the framework itself.
 */
class TransitionTracker(
    private val logger: ValidationLogger,
    private val recreateWatcher: RecreateWatcher,
) {

    // One record per live Activity or Fragment instance.
    //
    // The key is the component object itself, because "which instance is this" is the whole point:
    // two Activities of the same class are alive at once during a rotation, and one screen very
    // often holds several Fragments of the same class. Neither Activity nor Fragment overrides
    // equals/hashCode, so any Java map already compares these keys by identity.
    //
    // This is a WeakHashMap and not a HashMap for one reason: this map lives as long as the process
    // does, and a HashMap would keep every Activity ever created alive forever — exactly the leak
    // Check B is built to find. A WeakHashMap lets the garbage collector take a component as soon
    // as the app itself has let go of it, and the entry disappears along with it. That works only
    // because ComponentRecord stores Strings and numbers and never points back at the component; a
    // record holding its own key would keep the entry alive and undo all of this.
    //
    // Entries are deliberately not removed at onDestroy. The record has to outlive the destroy so
    // that a callback arriving afterwards still finds it and can be reported instead of quietly
    // starting a fresh record.
    //
    // No locking: every lifecycle callback that reaches this class runs on the main thread.
    private val records = WeakHashMap<Any, ComponentRecord>()

    /**
     * Records one ordinary lifecycle callback — onCreate, onStart, onResume, onPause or onStop —
     * for one component, and reports it if it does not follow on from the state the component was
     * already in.
     *
     * Called by [ActivityValidationCallbacks] and [FragmentValidationCallbacks] from inside the
     * matching framework callback. [instance] is the Activity or Fragment object, [name] its simple
     * class name, [kind] either "Activity" or "Fragment", [callbackName] the callback that arrived
     * and [next] the state that callback puts the component into.
     *
     * onDestroy is not handled here: it has end-of-life checks of its own, in the two functions
     * below.
     */
    fun onEvent(
        instance: Any,
        name: String,
        kind: String,
        callbackName: String,
        next: LifecycleState,
    ) {
        // A record is created here when one does not exist yet. Passing whether this callback is
        // the component's own onCreate is how the record learns whether it has watched the whole
        // life or joined half-way through.
        val record = recordFor(instance, name, kind, next == LifecycleState.CREATED)
        moveTo(record, next, callbackName)

        // The counts the end-of-life checks are built on. Kept here, in one place, so the callback
        // classes stay thin and there is only one spot where a count can be missed.
        when (next) {
            LifecycleState.STARTED -> record.startCount++
            LifecycleState.STOPPED -> record.stopCount++
            LifecycleState.RESUMED -> record.everResumed = true
            // The other states do not feed any count. Kotlin requires the branch, so it is empty.
            else -> {}
        }
    }

    /**
     * Records that an Activity was destroyed and runs every end-of-life check on it.
     *
     * Called by [ActivityValidationCallbacks] from onActivityDestroyed, which is the last callback
     * that will ever arrive for that instance. [configurationChange] comes from the Activity's own
     * isChangingConfigurations and is true when it is only being replaced because of a rotation or
     * a similar change.
     */
    fun onActivityDestroyed(activity: Any, name: String, configurationChange: Boolean) {
        val record = recordFor(activity, name, "Activity", false)
        moveTo(record, LifecycleState.DESTROYED, "onDestroy")
        reportEndOfLife(record)
        // Only Activities are counted for thrash: a Fragment being replaced repeatedly is ordinary
        // navigation, while an Activity class being rebuilt over and over is not.
        recreateWatcher.onActivityDestroyed(name, configurationChange)
    }

    /**
     * Records that a Fragment was destroyed, runs the same end-of-life checks as for an Activity,
     * and additionally checks that its view was torn down before it was.
     *
     * Called by [FragmentValidationCallbacks] from onFragmentDestroyed.
     */
    fun onFragmentDestroyed(fragment: Any, name: String) {
        val record = recordFor(fragment, name, "Fragment", false)
        moveTo(record, LifecycleState.DESTROYED, "onDestroy")
        reportEndOfLife(record)

        if (record.sawCreate && record.viewCreatedCount > record.viewDestroyedCount) {
            logger.report(
                record.label(),
                "was destroyed with ${record.viewCreatedCount} view(s) created but only " +
                    "${record.viewDestroyedCount} destroyed — onDestroyView never arrived for the " +
                    "last one, so anything that view still points at stays in memory",
            )
        }
    }

    /**
     * Notes that a Fragment just got a view.
     *
     * Called by [FragmentValidationCallbacks] from onFragmentViewCreated. A Fragment's view has its
     * own life that does not fit the state enum — the same Fragment can lose its view and be given
     * a new one while sitting on the back stack — so views are counted separately instead of being
     * squeezed into a state.
     */
    fun onFragmentViewCreated(fragment: Any, name: String) {
        val record = recordFor(fragment, name, "Fragment", false)
        record.viewCreatedCount++
    }

    /**
     * Notes that a Fragment's view was torn down.
     *
     * Called by [FragmentValidationCallbacks] from onFragmentViewDestroyed. The count it keeps is
     * compared against the created count in [onFragmentDestroyed].
     */
    fun onFragmentViewDestroyed(fragment: Any, name: String) {
        val record = recordFor(fragment, name, "Fragment", false)
        record.viewDestroyedCount++
    }

    /**
     * Finds the record for one component, creating it the first time that component is seen.
     *
     * Called by every public function above, at the top, so none of them has to worry about whether
     * a record exists yet. [fromCreateCallback] is true only when the caller is handling that
     * component's own onCreate; a record created from any other callback knows it joined late.
     */
    private fun recordFor(
        instance: Any,
        name: String,
        kind: String,
        fromCreateCallback: Boolean,
    ): ComponentRecord {
        val existing = records[instance]
        if (existing != null) {
            return existing
        }

        // identityHashCode is a number the runtime gives every object; two different objects
        // practically never share one, which is all that is needed to tell instances apart in the
        // log. Printed in hex only because it is shorter, and because the sample apps already print
        // their instances that way. Reading it does not keep the object alive.
        val instanceId = Integer.toHexString(System.identityHashCode(instance))

        val fresh = ComponentRecord(name, kind, instanceId, fromCreateCallback)
        records[instance] = fresh
        return fresh
    }

    /**
     * Decides whether the step from the component's current state to [next] is one the lifecycle
     * actually takes, reports it if it is not, and then stores the new state.
     *
     * Called by [onEvent] and by both destroy functions above, once per callback. This is the only
     * place [ComponentRecord.state] is ever written.
     */
    private fun moveTo(record: ComponentRecord, next: LifecycleState, callbackName: String) {
        val expected = when (record.state) {
            // We never saw the beginning of this component's life, so we cannot judge what should
            // come next. Accept anything and start judging from here on.
            LifecycleState.UNKNOWN -> true
            // Normally onStart follows onCreate. Straight to onDestroy is legal too: an Activity
            // that calls finish() inside its own onCreate is torn down without ever starting.
            LifecycleState.CREATED ->
                next == LifecycleState.STARTED || next == LifecycleState.DESTROYED
            // After onStart the component either comes to the foreground or goes away again.
            LifecycleState.STARTED ->
                next == LifecycleState.RESUMED || next == LifecycleState.STOPPED
            // The foreground is only ever left through onPause.
            LifecycleState.RESUMED -> next == LifecycleState.PAUSED
            // After onPause the component either comes back to the foreground or stops.
            LifecycleState.PAUSED ->
                next == LifecycleState.RESUMED || next == LifecycleState.STOPPED
            // A stopped component is either started again or destroyed.
            LifecycleState.STOPPED ->
                next == LifecycleState.STARTED || next == LifecycleState.DESTROYED
            // Nothing at all should arrive after onDestroy.
            LifecycleState.DESTROYED -> false
        }

        if (!expected) {
            logger.report(
                record.label(),
                "received $callbackName while it was ${record.state} — the lifecycle does not " +
                    "normally take that step",
            )
        }

        // Written even when the step was unexpected. Taking the new state as the truth means one
        // odd callback produces one finding; keeping the old state would make every callback after
        // it look wrong as well and bury the real problem under repeats.
        record.state = next
    }

    /**
     * Runs the two checks that can only be answered once a component is finished with: did it ever
     * reach the foreground, and did its starts and stops balance.
     *
     * Called by [onActivityDestroyed] and [onFragmentDestroyed], after the state has been moved to
     * destroyed.
     */
    private fun reportEndOfLife(record: ComponentRecord) {
        if (!record.sawCreate) {
            // The library was installed after this component's onCreate, so its counts are missing
            // their first half and its early callbacks were never seen. Reporting on that would be
            // reporting our own blind spot, not the app's behaviour.
            return
        }

        if (!record.everResumed) {
            // Worth knowing but not always a fault: a Fragment added behind another one, or a
            // screen that redirects somewhere else in onCreate, both die legitimately without ever
            // reaching the foreground.
            logger.report(record.label(), "was destroyed without ever reaching resumed")
        }

        if (record.startCount != record.stopCount) {
            logger.report(
                record.label(),
                "was destroyed with ${record.startCount} onStart(s) against " +
                    "${record.stopCount} onStop(s) — the two should always balance, so a callback " +
                    "was missed or arrived out of order",
            )
        }
    }
}
