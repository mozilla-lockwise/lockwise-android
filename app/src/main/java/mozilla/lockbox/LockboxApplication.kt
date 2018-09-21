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

val log: Logger = Logger("Lockbox")
class LockboxApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.addSink(AndroidLogSink())
    }
}