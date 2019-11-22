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
 *  Used to turn features in the app on and off, where true denotes that the feature is available
 *  when the app is run and false denotes that the feature is not available when the app is run.
 *
 *  Flags should be set to true while in debug mode or testing. Release builds should be
 *  set to false as to not show new features in development.
 */
object FeatureFlags {
    val CRUD_UPDATE_AND_DELETE = when {
        isDebug -> true
        isTesting -> true
        isRelease -> true
        else -> true
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

    /**
     * Prompt the user to save logins in autofill contexts.
     */
    val AUTOFILL_CAPTURE = when {
        isDebug -> true
        isTesting -> true
        isRelease -> true
        else -> true
    }

    /**
     * Use the Glean telemetry store.
     *
     * If false, the legacy telemetry-service is used.
     * If true, the glean telemetry service is used.
     *
     * Either way, the user can opt out of this from the settings.
     */
    val USE_GLEAN = when {
        isDebug -> true
        isTesting -> false
        isRelease -> false
        else -> false
    }
}
