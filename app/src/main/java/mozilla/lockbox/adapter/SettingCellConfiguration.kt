/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.adapter

import android.support.annotation.StringRes
import io.reactivex.Observable
import io.reactivex.functions.Consumer

abstract class SettingCellConfiguration(
    @StringRes open val title: Int? = null,
    @StringRes open val subtitle: Int? = null
)

class TextSettingConfiguration(
    @StringRes override val title: Int,
    @StringRes override val subtitle: Int? = null,
    val detailTextDriver: Observable<Int>,
    val clickListener: Consumer<Unit>
) : SettingCellConfiguration(title, subtitle)

class ToggleSettingConfiguration(
    @StringRes override val title: Int,
    override val subtitle: Int? = null,
    @StringRes val buttonTitle: Int? = null,
    val toggleDriver: Observable<Boolean>,
    val toggleObserver: Consumer<Boolean>
) : SettingCellConfiguration(title)

class AppVersionSettingConfiguration(
    val text: String
) : SettingCellConfiguration()
