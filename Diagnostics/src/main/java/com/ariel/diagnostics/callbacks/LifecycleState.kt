package com.ariel.diagnostics.callbacks

/**
 * The lifecycle state one Activity or Fragment instance is in, as far as this feature has seen it.
 */
enum class LifecycleState {

    /** The first callback seen for this instance was not its onCreate. Any next state is accepted. */
    UNKNOWN,

    /** onCreate has arrived. The component exists but is not visible yet. */
    CREATED,

    /** onStart has arrived. The component is visible but not necessarily in front of the user. */
    STARTED,

    /** onResume has arrived. The component is in the foreground and taking input. */
    RESUMED,

    /** onPause has arrived. The component is on its way out of the foreground, still visible. */
    PAUSED,

    /** onStop has arrived. The component is no longer visible but still exists. */
    STOPPED,

    /** onDestroy has arrived. Nothing more should ever arrive for this instance. */
    DESTROYED,
}
