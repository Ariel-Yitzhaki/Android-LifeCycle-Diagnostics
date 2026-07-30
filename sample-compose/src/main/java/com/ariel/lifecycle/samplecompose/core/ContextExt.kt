package com.ariel.lifecycle.samplecompose.core

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** Unwraps the Activity behind a Compose `LocalContext`, which is usually a ContextWrapper. */
fun Context.findActivity(): Activity {
    var context: Context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    error("No Activity in the context chain of $this")
}
