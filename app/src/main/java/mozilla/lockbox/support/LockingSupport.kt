package mozilla.lockbox.support

import android.os.SystemClock

interface LockingSupport {
    val systemTimeElapsed: Long
}

class SystemLockingSupport : LockingSupport {
    override val systemTimeElapsed: Long
        get() = SystemClock.elapsedRealtime()
}