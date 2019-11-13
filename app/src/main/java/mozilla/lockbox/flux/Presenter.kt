/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.flux

import androidx.annotation.CallSuper
import io.reactivex.disposables.CompositeDisposable

abstract class Presenter {
    val compositeDisposable: CompositeDisposable = CompositeDisposable()

    open fun onViewReady() {
        // NOOP
    }

    @CallSuper
    open fun onDestroy() {
        compositeDisposable.clear()
    }

    @CallSuper
    open fun onResume() {
        // NOOP
    }

    @CallSuper
    open fun onPause() {
        // NOOP
    }

    @CallSuper
    fun onStop() {
        // NOOP
    }

    /**
     * Called by the fragment when the back button is pressed.
     *
     * @return `true` if the back button event has been handled by the presenter.
     * By default, returns false, in which case Android handles the event.
     */
    open fun onBackPressed(): Boolean = false
}
