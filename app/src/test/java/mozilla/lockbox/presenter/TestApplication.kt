package mozilla.lockbox.presenter

import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.LockboxApplication
import mozilla.lockbox.log

@ExperimentalCoroutinesApi
class TestApplication : LockboxApplication() {

    init {
        log.info("Using TestApplication")
    }

    override val unitTesting = true
}