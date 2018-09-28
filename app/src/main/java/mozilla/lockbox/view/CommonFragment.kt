/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.view

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.View
import mozilla.lockbox.flux.Presenter

open class CommonFragment : Fragment() {
    lateinit var presenter: Presenter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter.onViewReady()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        presenter.onResume()
    }
}