/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.lockbox.presenter

import android.view.MenuItem
import io.reactivex.Observable

interface ListEntriesProtocol {
    // Protocol implementations
    val drawerItemSelections: Observable<MenuItem>

}

class ListEntriesPresenter(private val protocol: ListEntriesProtocol) {
    fun onViewReady() {
        // TODO: register for drawer item selections

        // TODO: register for list entry selections
    }
}