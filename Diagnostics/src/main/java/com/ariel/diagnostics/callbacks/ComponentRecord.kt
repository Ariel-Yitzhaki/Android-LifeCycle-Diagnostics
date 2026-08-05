package com.ariel.diagnostics.callbacks

/**
 * What this feature remembers about one Activity or Fragment instance while it is alive: its state
 * and a few counts that only mean something once it is destroyed.
 *
 * Nothing in here points at the component itself, only at values copied out of it — see
 * TransitionTracker for why that matters.
 */
class ComponentRecord(

    /** Simple class name of the component, for example "HomeActivity". */
    val name: String,

    /** Either "Activity" or "Fragment". Used only when printing. */
    val kind: String,

    /** Short hexadecimal id that tells two live instances of the same class apart in the log. */
    val instanceId: String,

    /**
     * True when this record was created by the component's own onCreate, so the whole life has been
     * watched. When false, the end-of-life checks are skipped because the counts below are missing
     * their first half.
     */
    val sawCreate: Boolean,
) {

    /** The state the last callback we saw put this component into. */
    var state = LifecycleState.UNKNOWN

    /** How many times onStart has arrived for this instance. */
    var startCount = 0

    /** How many times onStop has arrived for this instance. Should equal [startCount] at the end. */
    var stopCount = 0

    /** True once onResume has arrived at least once. */
    var everResumed = false

    /** Fragments only: how many times onFragmentViewCreated has arrived. */
    var viewCreatedCount = 0

    /** Fragments only: how many times onFragmentViewDestroyed has arrived. */
    var viewDestroyedCount = 0

    // Builds the name this component is printed under, for example "HomeActivity@3f2a1b (Activity)".
    fun label(): String {
        return "$name@$instanceId ($kind)"
    }
}
