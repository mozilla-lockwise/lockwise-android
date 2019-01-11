/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import mozilla.lockbox.action.LifecycleAction
import mozilla.lockbox.flux.Dispatcher

class ApplicationPresenter(
    val dispatcher: Dispatcher = Dispatcher.shared
) : DefaultLifecycleObserver {
    override fun onCreate(owner: LifecycleOwner) {
        dispatcher.dispatch(LifecycleAction.Startup)
    }

    override fun onPause(owner: LifecycleOwner) {
        dispatcher.dispatch(LifecycleAction.Background)
    }

    override fun onResume(owner: LifecycleOwner) {
        dispatcher.dispatch(LifecycleAction.Foreground)
    }
}