/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import mozilla.lockbox.presenter.AutofillFilterBackdropPresenter
import mozilla.lockbox.presenter.AutofillFilterBackdropView

class AutofillFilterBackdropFragment : Fragment(), AutofillFilterBackdropView {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        presenter = AutofillFilterBackdropPresenter(this)
        presenter.onViewReady()
        return super.onCreateView(inflater, container, savedInstanceState)
    }
}