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