/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.model

import mozilla.components.service.fxa.OAuthInfo
import org.json.JSONObject

private val emptyString = ""

class SyncCredentials(
    private val oauthInfo: OAuthInfo,
    val tokenServerURL: String,
    val scope: String
) {
    private val scopedKey: JSONObject? by lazy {
        return@lazy oauthInfo.keys?.let {
            val keysObject = JSONObject(it)
            keysObject[scope] as JSONObject
        } ?: null
    }

    // The following three properties should only be used when we have checked that the
    // credentials are syntactically valid, by calling `isValid`.
    val accessToken: String = oauthInfo.accessToken ?: emptyString
    val kid: String
        get() = scopedKey?.optString("kid", emptyString) ?: emptyString
    val syncKey: String
        get() = scopedKey?.optString("k", emptyString) ?: emptyString

    val isValid: Boolean
        get() {
            // unpacking JSON is annoying.
            val scopedKey = scopedKey ?: return false
            val accessToken = oauthInfo.accessToken ?: return false
            return listOf("k", "kid").all { !scopedKey.optString(it, emptyString).isEmpty() }
        }
}