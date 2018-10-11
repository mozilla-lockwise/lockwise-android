/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.flux

import android.support.annotation.CallSuper
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
}
