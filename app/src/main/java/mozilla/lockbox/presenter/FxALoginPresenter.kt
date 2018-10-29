/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import java.net.URL

interface FxALoginView {
    fun loadURL(url: URL)
}

class FxALoginPresenter(
    private val view: FxALoginView,
    private val dispatcher: Dispatcher = Dispatcher.shared
) : Presenter() {
    override fun onViewReady() {

    }
}