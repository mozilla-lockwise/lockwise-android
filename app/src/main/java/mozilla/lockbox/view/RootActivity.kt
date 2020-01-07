/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.view

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.R
import mozilla.lockbox.action.ToastNotificationAction
import mozilla.lockbox.presenter.AppRoutePresenter
import mozilla.lockbox.support.assertOnUiThread
import mozilla.lockbox.support.isDebug

@ExperimentalCoroutinesApi
class RootActivity : AppCompatActivity() {
    private var presenter: AppRoutePresenter = AppRoutePresenter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_root)
        if (!isDebug()) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        presenter.onViewReady()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        presenter.onPause()
    }

    override fun onResume() {
        super.onResume()
        presenter.onResume()
    }

    override fun onStop() {
        super.onStop()
        presenter.onStop()
    }

    override fun onBackPressed() {
        if (!presenter.onBackPressed()) {
            super.onBackPressed()
        }
    }

    fun showToastNotification(action: ToastNotificationAction) {
        assertOnUiThread()

        val toast = Toast(this)
        toast.duration = Toast.LENGTH_SHORT
        val container = window.decorView.rootView as ViewGroup
        toast.view = layoutInflater.inflate(R.layout.toast_view, container, false)
        toast.setGravity(Gravity.FILL_HORIZONTAL or Gravity.BOTTOM, 0, 0)

        // set text
        val view = toast.view.findViewById(R.id.message) as TextView
        view.text = action.viewModel.text ?: resources.getString(action.viewModel.strId!!)

        // set icon
        val image = toast.view.findViewById(R.id.icon) as ImageView
        image.setImageResource(action.viewModel.img)

        toast.show()
    }
}