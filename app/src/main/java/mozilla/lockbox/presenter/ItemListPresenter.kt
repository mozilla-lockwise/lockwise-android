/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.view.MenuItem
import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import mozilla.components.support.base.log.logger.Logger
import mozilla.lockbox.R
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.flux.Dispatcher

import mozilla.lockbox.flux.Presenter

interface ListEntriesProtocol {
    val drawerItemSelections: Observable<MenuItem>

    // TODO: Item list selection
}

class ListEntriesPresenter(private val protocol: ListEntriesProtocol, private val dispatcher: Dispatcher = Dispatcher.shared) : Presenter() {
    override fun onViewReady() {
        // TODO: register for drawer item selections

        // TODO: register for item list selections
        protocol.drawerItemSelections.subscribe { menuItem ->
            when (menuItem.itemId) {
                R.id.goto_settings -> {
                    dispatcher.dispatch(RouteAction.SETTING_LIST)
                }
                else -> {
                    Logger.warn("Menu ${menuItem.title} Unimplemented")
                }
            }
        }.addTo(compositeDisposable)
    }
}