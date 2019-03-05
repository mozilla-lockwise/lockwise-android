/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import io.reactivex.observers.TestObserver
import mozilla.lockbox.action.AutofillAction
import mozilla.lockbox.action.LifecycleAction
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import org.junit.Before
import org.junit.Test

class AutofillStoreTest {

    private val dispatcher = Dispatcher()
    private val autofillActionObserver = TestObserver.create<Action>()

    val subject = AutofillStore(dispatcher)

    @Before
    fun setUp() {
        subject.autofillActions.subscribe(autofillActionObserver)
    }

    @Test
    fun `dispatched autofill actions`() {
        val action = AutofillAction.Cancel
        dispatcher.dispatch(action)

        autofillActionObserver.assertValue(action)
    }

    @Test
    fun `dispatched non-autofillactions`() {
        val action = LifecycleAction.UserReset
        dispatcher.dispatch(action)

        autofillActionObserver.assertEmpty()
    }
}