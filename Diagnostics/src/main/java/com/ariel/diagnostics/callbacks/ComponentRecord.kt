package com.ariel.diagnostics.callbacks

/**
 * Everything this feature remembers about one single Activity or Fragment instance while it is
 * alive: which state it is in and a few counts that only mean something once it is destroyed.
 *
 * This is a plain class and not a data class on purpose. Its fields change all the way through the
 * component's life, and the equals/hashCode a data class generates compares by field value, which
 * would say two different Activities that happen to be in the same state are the same thing. This
 * record belongs to one instance and only to that instance.
 *
 * Nothing in here points at the Activity or Fragment itself, only at Strings and numbers copied out
 * of it. [TransitionTracker] explains why that matters.
 */
class ComponentRecord(

    /** Simple class name of the component, for example "HomeActivity". */
    val name: String,

    /** Either "Activity" or "Fragment". Used only when printing, to make findings readable. */
    val kind: String,

    /**
     * A short hexadecimal id that tells two live instances of the same class apart in the log. It
     * is derived from the instance once, at the moment this record is created, and is just a number
     * afterwards.
     */
    val instanceId: String,

    /**
     * True when this record was created by the component's own onCreate, so we have watched the
     * whole life from its start.
     *
     * When it is false the library was installed part-way through this component's life and the
     * counts below are missing their first half. The end-of-life checks are skipped in that case
     * rather than reporting a problem we caused ourselves.
     */
    val sawCreate: Boolean,
) {

    /** The state the last callback we saw put this component into. */
    var state = LifecycleState.UNKNOWN

    /** How many times onStart has arrived for this instance. */
    var startCount = 0

    /** How many times onStop has arrived for this instance. Should equal [startCount] at the end. */
    var stopCount = 0

    /** True once onResume has arrived at least once. A component that dies with this still false never made it to the foreground. */
    var everResumed = false

    /** Fragments only: how many times onFragmentViewCreated has arrived. Always 0 for Activities. */
    var viewCreatedCount = 0

    /** Fragments only: how many times onFragmentViewDestroyed has arrived. Always 0 for Activities. */
    var viewDestroyedCount = 0

    /**
     * Builds the name this component is printed under, for example "HomeActivity@3f2a1b (Activity)".
     *
     * Called by [TransitionTracker] every time it has a finding to report about this component. It
     * is built fresh each time rather than stored, because it is only needed on the rare lines that
     * actually report something.
     */
    fun label(): String {
        return "$name@$instanceId ($kind)"
    }
}
