package com.ariel.diagnostics

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import com.ariel.diagnostics.callbacks.CallbackValidation
import com.ariel.diagnostics.leaks.LeakDetection
import com.ariel.diagnostics.lifecycle.LifecycleDiagnostics

/**
 * Turns all three features on with no code in the app at all.
 *
 * This provider is declared in the library's own AndroidManifest.xml, and a library manifest is
 * merged into the manifest of every app that depends on the library. So an app gets the three
 * features by adding the dependency and nothing else: no Application subclass, no android:name, no
 * install() call.
 *
 * A ContentProvider is used because of when the framework creates one. Starting a process goes:
 * Application object constructed and given its base context, then every provider in the merged
 * manifest gets onCreate(), then Application.onCreate(). That middle step is the earliest point a
 * library can run code without the app's help, and it is before any Activity can exist, which is
 * what these features need — see the note on the missing start time in ActivityTimingCallbacks.
 *
 * None of the query/insert/delete/update methods are ever called. They are abstract on
 * ContentProvider, so a concrete class has to supply a body whether it means anything or not.
 *
 * Two things this deliberately does not do:
 *
 * - It does not run in a process other than the default one. A provider declared without
 *   android:process is only created in the app's main process, so an app with an android:process
 *   component should call the three install() methods from its own Application.onCreate for those
 *   processes. Each install() is idempotent, so doing that unconditionally is also fine.
 * - It offers no way to pick which features run. An app that wants that can drop this provider with
 *   `tools:node="remove"` on a matching <provider> in its own manifest and call install() by hand.
 */
class DiagnosticsInitProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        // A provider's context is the Application, but applicationContext is asked for rather than
        // assumed. The Application is fully constructed by now; only Application.onCreate has not
        // run yet, and none of the three features need it to have run.
        val application = context?.applicationContext as? Application ?: return false

        // The same three calls an app used to make by hand, in the same order.
        LifecycleDiagnostics.install(application)
        CallbackValidation.install(application)
        LeakDetection.install(application)
        return true
    }

    // ---- Required but unused -------------------------------------------------------------------

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
