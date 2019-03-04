/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.WindowManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.R
import mozilla.lockbox.presenter.RoutePresenter
import mozilla.lockbox.support.isDebug

@ExperimentalCoroutinesApi
class RootActivity : AppCompatActivity() {
    private var presenter: RoutePresenter = RoutePresenter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_root)
        if (!isDebug()) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        presenter.onViewReady()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
    }
}
