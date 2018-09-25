/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox

import android.annotation.SuppressLint
import android.app.Application
import io.sentry.Sentry
import io.sentry.android.AndroidSentryClientFactory
import io.sentry.event.BreadcrumbBuilder
import io.sentry.event.UserBuilder
import mozilla.components.support.base.log.Log
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.base.log.sink.AndroidLogSink

val log: Logger = Logger("Lockbox")
class LockboxApplication : Application() {
    @SuppressLint("AuthLeak")
    override fun onCreate() {
        super.onCreate()
        Log.addSink(AndroidLogSink())

        // Set up using Sentry DSN (client key) from the Project Settings page on Sentry
        val ctx = this.applicationContext
        val sentryDsn = "https://19558af5301f43e1a95ab4b8ceae663b:d3f7f6dfa9114ef0ba58accb8d8cc1ad@sentry.prod.mozaws.net/401?options"
        Sentry.init(sentryDsn, AndroidSentryClientFactory(ctx))
    }
}

class MyClass {
    /**
     * An example method that throws an exception.
     */
    internal fun unsafeMethod() {
        throw UnsupportedOperationException("You shouldn't call this!")
    }

    /**
     * Note that the ``Sentry.init`` method must be called before the static API
     * is used, otherwise a ``NullPointerException`` will be thrown.
     */
    internal fun logWithStaticAPI() {
        /*
         Record a breadcrumb in the current context which will be sent
         with the next event(s). By default the last 100 breadcrumbs are kept.
         */
        Sentry.getContext().recordBreadcrumb(
            BreadcrumbBuilder().setMessage("User made an action").build()
        )

        // Set the user in the current context.
        Sentry.getContext().user = UserBuilder().setEmail("hello@sentry.io").build()

        /*
         This sends a simple event to Sentry using the statically stored instance
         that was created in the ``main`` method.
         */
        Sentry.capture("This is a test")

        try {
            unsafeMethod()
        } catch (e: Exception) {
            // This sends an exception event to Sentry using the statically stored instance
            // that was created in the ``main`` method.
            Sentry.capture(e)
        }
    }
}
