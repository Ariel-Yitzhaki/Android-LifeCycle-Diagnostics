package com.ariel.diagnostics

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import com.ariel.diagnostics.blocking.MainThreadBlocking
import com.ariel.diagnostics.callbacks.CallbackValidation
import com.ariel.diagnostics.leaks.LeakDetection
import com.ariel.diagnostics.lifecycle.LifecycleDiagnostics

/**
 * Turns all four features on with no code in the app at all. Declared in the library's own manifest,
 * which is merged into every app that depends on the library.
 *
 * A ContentProvider is used because of when the framework creates one: after the Application object
 * is constructed but before Application.onCreate, which is the earliest a library can run code
 * without the app's help and before any Activity can exist.
 *
 * Two deliberate limits. It only runs in the default process, so an app with an android:process
 * component should call the four install() methods itself for those processes — each is idempotent.
 * And it offers no way to pick which features run; an app that wants that can drop this provider
 * with `tools:node="remove"` in its own manifest and call install() by hand.
 */
class DiagnosticsInitProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        val application = context?.applicationContext as? Application ?: return false

        LifecycleDiagnostics.install(application)
        CallbackValidation.install(application)
        LeakDetection.install(application)
        MainThreadBlocking.install(application)
        return true
    }

    // The methods below are abstract on ContentProvider but are never called for this provider.

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0
}
