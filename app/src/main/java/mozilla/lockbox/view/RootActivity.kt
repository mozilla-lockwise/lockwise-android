/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.view

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import mozilla.lockbox.R
import mozilla.lockbox.presenter.RoutePresenter

@kotlinx.coroutines.ExperimentalCoroutinesApi
class RootActivity : AppCompatActivity() {
    private var presenter: RoutePresenter = RoutePresenter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_root)

        presenter.onViewReady()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
    }
}
