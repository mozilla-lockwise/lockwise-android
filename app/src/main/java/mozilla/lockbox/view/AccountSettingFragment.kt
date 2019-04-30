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
import com.jakewharton.rxbinding2.view.clicks
import com.squareup.picasso.Picasso
import io.reactivex.Observable
import jp.wasabeef.picasso.transformations.CropCircleTransformation
import kotlinx.android.synthetic.main.fragment_account_setting.view.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.R
import mozilla.lockbox.presenter.AccountSettingPresenter
import mozilla.lockbox.presenter.AccountSettingView

@ExperimentalCoroutinesApi
class AccountSettingFragment : BackableFragment(), AccountSettingView {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        presenter = AccountSettingPresenter(this)
        val view = inflater.inflate(R.layout.fragment_account_setting, container, false)
        view.profileImage.clipToOutline = true
        val appName = getString(R.string.app_name)
        view.disconnectButton.text = getString(R.string.disconnect_button, appName)
        view.disconnectDisclaimer.text = getString(R.string.disconnect_disclaimer, appName)
        return view
    }

    override fun setDisplayName(text: String) {
        view!!.displayName.text = text
    }

    override fun setAvatarFromURL(url: String) {
        Picasso.get()
            .load(url)
            .placeholder(R.drawable.ic_avatar_placeholder)
            .resize(80, 80)
            .transform(CropCircleTransformation())
            .into(view!!.profileImage)
    }

    override val disconnectButtonClicks: Observable<Unit>
        get() = view!!.disconnectButton.clicks()
}