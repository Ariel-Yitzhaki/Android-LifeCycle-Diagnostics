package com.ariel.diagnostics.callbacks

/**
 * The lifecycle state one Activity or Fragment instance is in, as far as this feature has seen it.
 *
 * Activities and Fragments share this enum because the six callbacks that move them between states
 * have the same names and arrive in the same order for both.
 */
enum class LifecycleState {

    /**
     * Nothing has been seen for this instance yet. A record starts here when the first callback we
     * receive for a component is not its onCreate, which happens when the library is installed
     * while the component is already part-way through its life. From this state every next state is
     * accepted without complaint, because we genuinely do not know what came before.
     */
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
