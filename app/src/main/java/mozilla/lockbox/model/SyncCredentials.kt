/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.model

import mozilla.components.service.fxa.OAuthInfo
import mozilla.components.service.fxa.OAuthScopedKey

private val emptyString = ""

class SyncCredentials(
    private val oauthInfo: OAuthInfo,
    val tokenServerURL: String,
    private val scope: String
) {
    private val scopedKey: OAuthScopedKey? by lazy {
        oauthInfo.keys?.get(scope)
    }

    // The following three properties should only be used when we have checked that the
    // credentials are syntactically valid, by calling `isValid`.
    val accessToken: String = oauthInfo.accessToken
    val kid: String
        get() = scopedKey?.kid ?: emptyString
    val syncKey: String
        get() = scopedKey?.k ?: emptyString

    val isValid: Boolean
        get() = scopedKey != null
}