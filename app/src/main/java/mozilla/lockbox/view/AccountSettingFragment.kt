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
import kotlinx.android.synthetic.main.fragment_account_setting.view.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.components.concept.sync.Avatar
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
        val appLabel = getString(R.string.app_label)
        view.disconnectButton.text = getString(R.string.disconnect_button, appLabel)
        view.disconnectDisclaimer.text = getString(R.string.disconnect_disclaimer, appLabel)
        return view
    }

    override fun setDisplayName(text: String) {
        requireView().displayName.text = text
    }

    override fun setAvatar(avatar: Avatar) {
        val url =
            if (avatar.url.isNullOrEmpty() || avatar.url == resources.getString(R.string.default_avatar_url)) {
                null
            } else {
                avatar.url
            }

        Picasso.get()
            .load(url)
            .placeholder(R.drawable.ic_avatar_placeholder)
            .into(requireView().profileImage)
    }

    override val disconnectButtonClicks: Observable<Unit>
        get() = requireView().disconnectButton.clicks()
}