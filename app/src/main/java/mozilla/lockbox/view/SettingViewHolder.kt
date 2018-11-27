/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.view

import android.support.annotation.StringRes
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.Switch
import com.jakewharton.rxbinding2.widget.checkedChanges
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.list_cell_setting_appversion.view.appVersion
import kotlinx.android.synthetic.main.list_cell_setting_text.view.description
import kotlinx.android.synthetic.main.list_cell_setting_text.view.settingSelection
import kotlinx.android.synthetic.main.list_cell_setting_toggle.view.subtitle
import kotlinx.android.synthetic.main.list_cell_setting_toggle.view.button
import kotlinx.android.synthetic.main.list_cell_setting_toggle.view.title
import kotlinx.android.synthetic.main.list_cell_setting_toggle.view.toggle
import mozilla.lockbox.R
import mozilla.lockbox.R.string.empty_string

abstract class SettingViewHolder(override val containerView: View) :
    RecyclerView.ViewHolder(containerView),
    LayoutContainer {
    var disposable: Disposable? = null
}

class TextSettingViewHolder(val view: View) : SettingViewHolder(view) {
    var title: Int = empty_string
        set(@StringRes value) {
            field = value
            view.description.setText(value)
        }

    var detailTextRes: Int = empty_string
        set(@StringRes value) {
            field = value
            view.settingSelection.setText(value)
        }
}

class ToggleSettingViewHolder(val view: View) : SettingViewHolder(view) {
    var title: Int = empty_string
        set(@StringRes value) {
            field = value
            view.title.setText(value)
        }
    var subtitle: Int = empty_string
        set(@StringRes value) {
            field = value
            if (value != empty_string) {
                view.subtitle.setText(value)
                view.subtitle.visibility = View.VISIBLE
            } else {
                view.subtitle.visibility = View.GONE
            }
        }

    var buttonTitle: Int = empty_string
        set(@StringRes value) {
            field = value
            if (value != empty_string) {
                view.button.text = view.resources.getString(value)
                view.button.contentDescription = view.resources.getString(R.string.learn_more_description)
                view.button.visibility = View.VISIBLE
            } else {
                view.button.visibility = View.GONE
            }
        }

    var toggle: Switch = view.toggle

    val toggleValueChanges: Observable<Boolean> = view.toggle.checkedChanges()
}

class AppVersionSettingViewHolder(val view: View) : SettingViewHolder(view) {
    var text: String? = null
        set(value) {
            field = value
            view.appVersion.text = value
        }
}