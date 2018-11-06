/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.support

import mozilla.components.service.fxa.Profile

interface FxAProfile {
    val uid: String?
    val email: String?
    val displayName: String?
    val avatar: String?
}

fun Profile.toFxAProfile() = object : FxAProfile {
    private val profile = this@toFxAProfile
    override val uid = profile.uid
    override val email = profile.email
    override val displayName = profile.displayName
    override val avatar = profile.avatar
}
