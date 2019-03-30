/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.adapter

import androidx.annotation.StringRes
import io.reactivex.Observable
import io.reactivex.functions.Consumer

abstract class SettingCellConfiguration(
    @StringRes open val title: Int? = null,
    @StringRes open val subtitle: Int? = null,
    @StringRes open val contentDescription: Int
)

class TextSettingConfiguration(
    @StringRes override val title: Int,
    @StringRes override val subtitle: Int? = null,
    @StringRes override val contentDescription: Int,
    val detailTextDriver: Observable<Int>,
    val clickListener: Consumer<Unit>
) : SettingCellConfiguration(title, subtitle, contentDescription)

class ToggleSettingConfiguration(
    @StringRes override val title: Int,
    override val subtitle: Int? = null,
    @StringRes override val contentDescription: Int,
    @StringRes val buttonTitle: Int? = null,
    val buttonObserver: Consumer<Unit>? = null,
    val toggleDriver: Observable<Boolean>,
    val toggleObserver: Consumer<Boolean>
) : SettingCellConfiguration(title = title, contentDescription = contentDescription)

class AppVersionSettingConfiguration(
    @StringRes override val title: Int,
    val appVersion: String,
    val buildNumber: Int,
    @StringRes override val contentDescription: Int
) : SettingCellConfiguration(contentDescription = contentDescription)
