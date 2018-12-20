/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import mozilla.lockbox.DisposingTest
import mozilla.lockbox.action.LifecycleAction
import mozilla.lockbox.flux.Dispatcher
import org.junit.Test

class LifecycleStoreTest : DisposingTest() {
    @Test
    fun testLifecycleFilter() {
        val dispatcher = Dispatcher()
        val subject = LifecycleStore(dispatcher)

        val lifecycleObserver = createTestObserver<LifecycleAction>()
        subject.lifecycleEvents.subscribe(lifecycleObserver)

        dispatcher.dispatch(LifecycleAction.Startup)
        dispatcher.dispatch(LifecycleAction.Upgrade)
        dispatcher.dispatch(LifecycleAction.Foreground)
        dispatcher.dispatch(LifecycleAction.Background)

        lifecycleObserver.assertValueSequence(listOf(
            LifecycleAction.Startup,
            LifecycleAction.Upgrade,
            LifecycleAction.Foreground,
            LifecycleAction.Background
        ))
    }
}