/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.view

import android.os.Bundle
import android.support.v7.preference.PreferenceFragmentCompat
import mozilla.lockbox.R
import mozilla.lockbox.BuildConfig

class SettingFragment : PreferenceFragmentCompat() {
    private val versionNumber = BuildConfig.VERSION_NAME

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.settings)

        val appVersion = findPreference("app_version")
        appVersion.setTitle("App Version: $versionNumber")
    }
}
