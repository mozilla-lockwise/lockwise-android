package mozilla.lockbox.support

import android.os.SystemClock

interface LockingSupport {
    var systemTimeElapsed: Long
    var currentBootId: String
}

class SystemLockingSupport : LockingSupport {
    override var systemTimeElapsed: Long = 0L
        get() = SystemClock.elapsedRealtime()
    override var currentBootId: String = ""
        get() = SimpleFileReader().readContents(Constant.App.bootIDPath).trimEnd()
}