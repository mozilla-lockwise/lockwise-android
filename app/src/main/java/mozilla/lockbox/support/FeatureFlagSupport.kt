/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.support

import mozilla.lockbox.BuildConfig

object FeatureFlagSupport {
    const val isDebug = BuildConfig.BUILD_TYPE == "debug"
    const val isRelease = BuildConfig.BUILD_TYPE == "release"
    val isTesting = isTesting()
}
