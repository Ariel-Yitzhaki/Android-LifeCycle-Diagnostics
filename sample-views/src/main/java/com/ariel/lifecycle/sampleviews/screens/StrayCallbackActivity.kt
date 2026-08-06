package com.ariel.lifecycle.sampleviews.screens

import android.os.Bundle
import com.ariel.lifecycle.sampleviews.ui.SimpleScreenActivity

/**
 * FAULT: hands the framework's own `onStart` out a second time, by hand.
 *
 * Calling a lifecycle callback yourself is not something anyone sets out to do, but it is what an
 * app ends up doing when it forwards lifecycle to a helper, a presenter or a base class and the
 * forwarding overlaps with the framework's. `Activity.onStart` dispatches to every registered
 * `ActivityLifecycleCallbacks`, so a stray call is indistinguishable, to a watcher, from the
 * framework having gone wrong.
 *
 * Two findings come out of one tap: the step itself is one the lifecycle does not take, and the
 * onStart it added leaves the start and stop counts unbalanced when the screen is destroyed.
 */
class StrayCallbackActivity : SimpleScreenActivity() {

    private var strayStarts = 0

    override val faultDescription =
        "FAULT — calls the framework's own onStart() a second time, by hand"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
        primaryButton("Deliver a stray onStart()") {
            deliverStrayStart()
            strayStarts++
            render()
        }
    }

    // super.onStart() is what dispatches to ActivityLifecycleCallbacks, so this is a real extra
    // onStart as far as anything watching the lifecycle can tell. It has to sit in a method of its
    // own: a super call cannot be written inside a lambda.
    private fun deliverStrayStart() {
        super.onStart()
    }

    private fun render() {
        setStatus("Stray onStart() calls delivered: $strayStarts")
        setNote(
            "Each tap reports a step out of order straight away. Press Back afterwards and the " +
                "screen is destroyed with more onStarts than onStops, which is a second, " +
                "separate finding."
        )
    }
}
