/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.model

import mozilla.components.service.fxa.OAuthScopedKey
import mozilla.lockbox.support.FxAOauthInfo

private val emptyString = ""

interface SyncCredentials {
    val isNew: Boolean

    val tokenServerURL: String
    // The following three properties should only be used when we have checked that the
    // credentials are syntactically valid, by calling `isValid`.
    val accessToken: String
    val kid: String
    val syncKey: String
    val isValid: Boolean
}

class FixedSyncCredentials(
    override val isNew: Boolean
) : SyncCredentials {
    override val accessToken: String = emptyString
    override val kid: String = emptyString
    override val syncKey: String = emptyString
    override val tokenServerURL: String = emptyString

    override val isValid: Boolean = true
}

class FxASyncCredentials(
    private val oauthInfo: FxAOauthInfo,
    override val tokenServerURL: String,
    private val scope: String,
    override val isNew: Boolean
) : SyncCredentials {
    private val scopedKey: OAuthScopedKey? by lazy {
        oauthInfo.keys?.get(scope)
    }

    // The following three properties should only be used when we have checked that the
    // credentials are syntactically valid, by calling `isValid`.
    override val accessToken: String = oauthInfo.accessToken
    override val kid: String
        get() = scopedKey?.kid ?: emptyString
    override val syncKey: String
        get() = scopedKey?.k ?: emptyString

    override val isValid: Boolean
        get() = scopedKey != null
}