/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.lockbox.presenter

import android.view.MenuItem
import io.reactivex.Observable
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.view.ListEntriesFragment

interface ListEntriesProtocol {
    // Protocol implementations
    val drawerItemClicks: Observable<MenuItem>

}

class ListEntriesPresenter(private val protocol: ListEntriesProtocol) {
    fun onViewReady() {
        // TODO: stuff
    }
}