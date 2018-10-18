/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.adapter

import io.reactivex.Observable

abstract class SettingCellConfiguration(
    open val title: String,
    open val subtitle: String? = null
)

class TextSettingConfiguration(
    override val title: String,
    override val subtitle: String? = null,
    val detailText: String
) : SettingCellConfiguration(title, subtitle)

class ToggleSettingConfiguration(
    override val title: String,
    override val subtitle: String? = null,
    val buttonTitle: String? = null,
    val toggle: Observable<Boolean>
) : SettingCellConfiguration(title)

class AppVersionSettingConfiguration(
    val text: String
) : SettingCellConfiguration(text)
