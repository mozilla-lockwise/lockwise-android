package mozilla.lockbox.support

import android.os.SystemClock

interface LockingSupport {
    var systemTimeElapsed: Long
}

class SystemLockingSupport : LockingSupport {
    override var systemTimeElapsed: Long = 0L
        get() = SystemClock.elapsedRealtime()
}