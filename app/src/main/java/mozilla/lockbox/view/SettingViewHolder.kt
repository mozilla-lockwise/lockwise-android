/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.view

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.Switch
import com.jakewharton.rxbinding2.widget.checkedChanges
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.list_cell_setting_appversion.view.*
import kotlinx.android.synthetic.main.list_cell_setting_text.view.*
import kotlinx.android.synthetic.main.list_cell_setting_toggle.view.*

abstract class SettingViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView),
    LayoutContainer {
    val compositeDisposable = CompositeDisposable()
}

class TextSettingViewHolder(val view: View) : SettingViewHolder(view) {
    var title: String? = null
        set(value) {
            field = value
            view.description.text = value
        }
    var detailText: String? = null
        set(value) {
            field = value
            view.settingSelection.text = value
        }
}

class ToggleSettingViewHolder(val view: View) : SettingViewHolder(view) {
    var title: String? = null
        set(value) {
            field = value
            view.title.text = value
        }
    var subtitle: String? = null
        set(value) {
            field = value
            value?.let {
                view.subtitle.text = value
                view.subtitle.visibility = View.VISIBLE
            } ?: run {
                view.subtitle.visibility = View.GONE
            }
        }

    var buttonTitle: String? = null
        set(value) {
            field = value
            value?.let {
                view.button.text = value
                view.button.visibility = View.VISIBLE
            } ?: run {
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