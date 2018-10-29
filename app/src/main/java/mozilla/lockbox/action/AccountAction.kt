/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.action

import mozilla.lockbox.flux.Action
import java.net.URL

sealed class AccountAction : Action {
    data class OauthRedirect(val url: URL) : AccountAction()
    object Clear : AccountAction()
}