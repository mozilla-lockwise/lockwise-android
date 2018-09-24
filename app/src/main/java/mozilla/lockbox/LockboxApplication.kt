/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox

import android.app.Application
import mozilla.components.support.base.log.Log
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.base.log.sink.AndroidLogSink
import io.sentry.android.AndroidSentryClientFactory
import io.sentry.Sentry

val log: Logger = Logger("Lockbox")
class LockboxApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.addSink(AndroidLogSink())

        // Set up using Sentry DSN (client key) from the Project Settings page on Sentry
        val ctx = this.applicationContext
        val sentryDsn = "https://19558af5301f43e1a95ab4b8ceae663b:d3f7f6dfa9114ef0ba58accb8d8cc1ad@sentry.prod.mozaws.net/401"
        Sentry.init(sentryDsn, AndroidSentryClientFactory(ctx))
    }
}