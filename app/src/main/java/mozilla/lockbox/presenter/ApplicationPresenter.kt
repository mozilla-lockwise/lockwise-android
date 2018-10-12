/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.arch.lifecycle.DefaultLifecycleObserver
import android.arch.lifecycle.LifecycleOwner
import mozilla.lockbox.action.LifecycleAction
import mozilla.lockbox.flux.Dispatcher

class ApplicationPresenter(
    val dispatcher: Dispatcher = Dispatcher.shared
) : DefaultLifecycleObserver {
    override fun onCreate(owner: LifecycleOwner) {
        Dispatcher.shared.dispatch(LifecycleAction.Startup)
    }

    override fun onPause(owner: LifecycleOwner) {
        Dispatcher.shared.dispatch(LifecycleAction.Background)
    }

    override fun onResume(owner: LifecycleOwner) {
        Dispatcher.shared.dispatch(LifecycleAction.Foreground)
    }
}