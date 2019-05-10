/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.view

import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.widget.Switch
import com.jakewharton.rxbinding2.view.clicks
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

private const val emptyString = ""

abstract class SettingViewHolder(override val containerView: View) :
    RecyclerView.ViewHolder(containerView),
    LayoutContainer {
    var disposable: Disposable? = null
    abstract var contentDescription: Int
}

class TextSettingViewHolder(val view: View) : SettingViewHolder(view) {

    override var contentDescription: Int = R.string.empty_string
        set(@StringRes value) {
            field = value
            view.description.contentDescription = view.resources.getString(value)
        }

    var title: Int = R.string.empty_string
        set(@StringRes value) {
            field = value
            view.description.setText(value)
        }

    var detailTextRes: Int = R.string.empty_string
        set(@StringRes value) {
            field = value
            view.settingSelection.setText(value)
        }
}

class ToggleSettingViewHolder(val view: View) : SettingViewHolder(view) {
    // toggle content description
    override var contentDescription: Int = R.string.empty_string
        set(@StringRes value) {
            view.toggle.contentDescription = view.resources.getString(value)
        }

    var title: Int = R.string.empty_string
        set(@StringRes value) {
            field = value
            view.title.setText(value)
        }
    var subtitle: Int = R.string.empty_string
        set(@StringRes value) {
            field = value
            if (value != R.string.empty_string) {
                val string = view.context.resources.getString(value)
                if (string.contains("%1\$s")) {
                    val appLabel = view.context.resources.getString(R.string.app_label)
                    view.subtitle.text = String.format(string, appLabel)
                } else {
                    view.subtitle.setText(value)
                }
                view.subtitle.visibility = View.VISIBLE
            } else {
                view.subtitle.visibility = View.GONE
            }
        }

    var buttonTitle: Int = R.string.empty_string
        set(@StringRes value) {
            field = value
            if (value != R.string.empty_string) {
                view.button.text = view.resources.getString(value)
                view.button.contentDescription = view.resources.getString(R.string.learn_more_description)
                view.button.visibility = View.VISIBLE
            } else {
                view.button.visibility = View.GONE
            }
        }

    var buttonClicks: Observable<Unit> = view.button.clicks()

    var toggle: Switch = view.toggle

    val toggleValueChanges: Observable<Boolean> = view.toggle.checkedChanges()
}

class AppVersionSettingViewHolder(val view: View) : SettingViewHolder(view) {

    override var contentDescription: Int = R.string.empty_string
        set(@StringRes value) {
            view.contentDescription = view.resources.getString(value)
        }

    var title: String = emptyString
        private set(value) {
            field = value
            view.appVersion.text = value
        }

    fun setTitle(@StringRes format: Int, version: String, buildNumber: Int = 0) {
        title = view.resources.getString(format, version, buildNumber)
    }
}