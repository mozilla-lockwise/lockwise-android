/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.model

import mozilla.components.concept.sync.AccessTokenInfo
import mozilla.components.concept.sync.OAuthScopedKey
import mozilla.lockbox.support.DataStoreSupport
import mozilla.lockbox.support.FixedDataStoreSupport
import mozilla.lockbox.support.FxASyncDataStoreSupport

private val emptyString = ""

interface SyncCredentials {
    val isNew: Boolean

    val tokenServerURL: String
    // The following three properties should only be used when we have checked that the
    // credentials are syntactically valid, by calling `isValid`.
    val accessToken: AccessTokenInfo
    val kid: String
    val syncKey: String
    val isValid: Boolean

    val support: DataStoreSupport
}

class FixedSyncCredentials(
    override val isNew: Boolean,
    override val accessToken: AccessTokenInfo = AccessTokenInfo(emptyString, emptyString, null, 0L),
    override val kid: String = emptyString,
    override val syncKey: String = emptyString,
    override val tokenServerURL: String = emptyString
) : SyncCredentials {
    override val isValid: Boolean = true

    override val support: DataStoreSupport = FixedDataStoreSupport.shared
}

class FxASyncCredentials(
    override val accessToken: AccessTokenInfo,
    override val tokenServerURL: String,
    override val isNew: Boolean
) : SyncCredentials {
    private val scopedKey: OAuthScopedKey? by lazy {
        accessToken.key
    }

    // The following three properties should only be used when we have checked that the
    // credentials are syntactically valid, by calling `isValid`.
    override val kid: String
        get() = scopedKey?.kid ?: emptyString
    override val syncKey: String
        get() = scopedKey?.k ?: emptyString

    override val isValid: Boolean
        get() = scopedKey != null

    override val support: DataStoreSupport = FxASyncDataStoreSupport.shared
}