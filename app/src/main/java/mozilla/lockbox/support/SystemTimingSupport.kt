package mozilla.lockbox.support

import android.os.SystemClock

interface SystemTimingSupport {
    val systemTimeElapsed: Long
}

class SystemSystemTimingSupport : SystemTimingSupport {
    override val systemTimeElapsed: Long
        get() = SystemClock.elapsedRealtime()
}