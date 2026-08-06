package com.ariel.diagnostics.callbacks

import java.util.WeakHashMap

/**
 * Keeps the current lifecycle state of every live Activity and Fragment, and reports any step that
 * does not follow the one before it, a component that died without ever reaching resumed,
 * unbalanced onStart/onStop counts, an Activity class that keeps being recreated, and a Fragment
 * whose view outlived its onDestroyView.
 */
class TransitionTracker(
    private val logger: ValidationLogger,
    private val recreateWatcher: RecreateWatcher,
) {

    // Keyed by the component itself, compared by identity since neither Activity nor Fragment
    // overrides equals/hashCode. Weak keys because this map lives as long as the process, which is
    // safe only because ComponentRecord never points back at its own key.
    //
    // Entries are not removed at onDestroy, so a callback arriving afterwards still finds the record
    // and can be reported instead of quietly starting a fresh one.
    private val records = WeakHashMap<Any, ComponentRecord>()

    // Records one ordinary lifecycle callback and reports it if it does not follow on from the
    // state the component was already in. next is the state callbackName puts the component into.
    //
    // onDestroy is not handled here; it has end-of-life checks of its own below.
    fun onEvent(
        instance: Any,
        name: String,
        kind: String,
        callbackName: String,
        next: LifecycleState,
    ) {
        val record = recordFor(instance, name, kind, next == LifecycleState.CREATED)
        moveTo(record, next, callbackName)

        when (next) {
            LifecycleState.STARTED -> record.startCount++
            LifecycleState.STOPPED -> record.stopCount++
            LifecycleState.RESUMED -> record.everResumed = true
            else -> {}
        }
    }

    // Records that an Activity was destroyed and runs every end-of-life check on it.
    fun onActivityDestroyed(activity: Any, name: String, configurationChange: Boolean) {
        val record = recordFor(activity, name, "Activity", false)
        moveTo(record, LifecycleState.DESTROYED, "onDestroy")
        reportEndOfLife(record)
        // Only Activities are counted for thrash: a Fragment being replaced repeatedly is ordinary
        // navigation.
        recreateWatcher.onActivityDestroyed(name, configurationChange)
    }

    // Records that a Fragment was destroyed, runs the same end-of-life checks as for an Activity,
    // and additionally checks that its view was torn down first.
    fun onFragmentDestroyed(fragment: Any, name: String) {
        val record = recordFor(fragment, name, "Fragment", false)
        moveTo(record, LifecycleState.DESTROYED, "onDestroy")
        reportEndOfLife(record)

        if (record.sawCreate && record.viewCreatedCount > record.viewDestroyedCount) {
            logger.report(
                record.label(),
                "was destroyed with ${record.viewCreatedCount} view(s) created but only " +
                    "${record.viewDestroyedCount} destroyed. onDestroyView never arrived for the " +
                    "last one, so anything that view still points at stays in memory",
            )
        }
    }

    fun onFragmentViewCreated(fragment: Any, name: String) {
        val record = recordFor(fragment, name, "Fragment", false)
        record.viewCreatedCount++
    }

    // Compared against the created count on destroy.
    fun onFragmentViewDestroyed(fragment: Any, name: String) {
        val record = recordFor(fragment, name, "Fragment", false)
        record.viewDestroyedCount++
    }

    // Finds the record for one component, creating it the first time that component is seen.
    // fromCreateCallback is true only when the caller is handling that component's own onCreate.
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

        // identityHashCode tells instances apart in the log without keeping the object alive.
        val instanceId = Integer.toHexString(System.identityHashCode(instance))

        val fresh = ComponentRecord(name, kind, instanceId, fromCreateCallback)
        records[instance] = fresh
        return fresh
    }

    // Decides whether the step from the component's current state to next is one the lifecycle
    // actually takes, reports it if it is not, and stores the new state. The only place
    // ComponentRecord.state is written.
    private fun moveTo(record: ComponentRecord, next: LifecycleState, callbackName: String) {
        val expected = when (record.state) {
            LifecycleState.UNKNOWN -> true
            // Straight to onDestroy is legal: an Activity that calls finish() inside its own
            // onCreate is torn down without ever starting.
            LifecycleState.CREATED ->
                next == LifecycleState.STARTED || next == LifecycleState.DESTROYED
            LifecycleState.STARTED ->
                next == LifecycleState.RESUMED || next == LifecycleState.STOPPED
            LifecycleState.RESUMED -> next == LifecycleState.PAUSED
            LifecycleState.PAUSED ->
                next == LifecycleState.RESUMED || next == LifecycleState.STOPPED
            LifecycleState.STOPPED ->
                next == LifecycleState.STARTED || next == LifecycleState.DESTROYED
            LifecycleState.DESTROYED -> false
        }

        if (!expected) {
            logger.report(
                record.label(),
                "received $callbackName while it was ${record.state}. The lifecycle does not " +
                    "normally take that step",
            )
        }

        // Written even when the step was unexpected, so one odd callback produces one finding
        // rather than making every callback after it look wrong too.
        record.state = next
    }

    // Runs the two checks that can only be answered once a component is finished with: was it built
    // and then thrown away without the user ever reaching it, and did its starts and stops balance.
    private fun reportEndOfLife(record: ComponentRecord) {
        if (!record.sawCreate) {
            // The library was installed after this component's onCreate, so its counts are missing
            // their first half.
            return
        }

        // Never having started is not the same as never having resumed, and only the second is
        // worth a finding.
        //
        // A component that never started was finished before the framework would have shown it,
        // which is what a launcher screen does when it decides in its own onCreate where to send
        // the user, and what a fragment does when it is replaced in the transaction that added it.
        // Nothing was inflated and nothing was wasted, and moveTo() above already treats that same
        // step as a legal one.
        //
        // Starting and then never resuming is the case that costs something: the view was inflated,
        // laid out and put on screen for a component the user never got to touch.
        if (record.startCount > 0 && !record.everResumed) {
            logger.report(
                record.label(),
                "was started and then destroyed without ever reaching resumed, so its view was " +
                    "built for a screen the user never got to use",
            )
        }

        if (record.startCount != record.stopCount) {
            logger.report(
                record.label(),
                "was destroyed with ${record.startCount} onStart(s) against " +
                    "${record.stopCount} onStop(s). The two should always balance, so a callback " +
                    "was missed or arrived out of order",
            )
        }
    }
}
