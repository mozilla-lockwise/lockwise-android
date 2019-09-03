/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.support

import mozilla.lockbox.support.FeatureFlagSupport.isDebug
import mozilla.lockbox.support.FeatureFlagSupport.isTesting
import mozilla.lockbox.support.FeatureFlagSupport.isRelease

/*
 *  Used to turn features in the app on and off, where true denotes that the feature is available when
 *  the app is run and false denotes that the feature is not available when the app is run.
 *
 *  Flags will be set to true while in debug mode or testing. Release builds will automatically be set
 *  to false as to not show new features in development.
 */
object FeatureFlags {
    val CRUD_DELETE = when {
        isDebug -> true
        isTesting -> true
        isRelease -> false
        else -> false
    }

    @Suppress("unused")
    val CRUD_EDIT = when {
        isDebug -> true
        isTesting -> true
        isRelease -> false
        else -> false
    }

    @Suppress("unused")
    val CRUD_CREATE = when {
        isDebug -> true
        isTesting -> true
        isRelease -> false
        else -> false
    }

    /**
     * Use existing apps' FxA credentials to login.
     *
     * Currently, only Fennec will detect the release version of Lockwise.
     * To see this work, you need a custom build of Fennec and Android Components.
     */
    val FXA_LOGIN_WITTH_AUTHPROVIDER = when {
        isDebug -> true
        isTesting -> false
        isRelease -> false
        else -> false
    }
}
