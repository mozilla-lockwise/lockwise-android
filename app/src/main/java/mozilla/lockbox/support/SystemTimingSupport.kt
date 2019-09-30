package mozilla.lockbox.support

import android.os.SystemClock

interface SystemTimingSupport {
    val systemTimeElapsed: Long
    val currentTimeMillis: Long
}

class DeviceSystemTimingSupport : SystemTimingSupport {
    companion object {
        val shared = DeviceSystemTimingSupport()
    }

    override val systemTimeElapsed: Long
        get() = SystemClock.elapsedRealtime()

    override val currentTimeMillis: Long
        get() = System.currentTimeMillis()
}

class TestSystemTimingSupport() : SystemTimingSupport {
    override var systemTimeElapsed: Long = 0L
    override var currentTimeMillis: Long = 0L

    constructor(existing: SystemTimingSupport) : this() {
        systemTimeElapsed = existing.systemTimeElapsed
        currentTimeMillis = existing.currentTimeMillis
    }

    fun advance(time: Long = 1L) {
        systemTimeElapsed += time
        currentTimeMillis += time
    }
}
