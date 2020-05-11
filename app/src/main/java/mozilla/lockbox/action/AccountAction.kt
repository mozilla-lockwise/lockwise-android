/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.action

import mozilla.components.service.fxa.sharing.ShareableAccount
import mozilla.lockbox.flux.Action

sealed class AccountAction : Action {
    object Reset : AccountAction()
    object UseTestData : AccountAction()
    data class OauthRedirect(val url: String) : AccountAction()
    data class AutomaticLogin(val account: ShareableAccount) : AccountAction()
}

sealed class ManageAccountDataAction(
    override val eventMethod: TelemetryEventMethod,
    override val eventObject: TelemetryEventObject
) : TelemetryAction {
    object DeleteUserData : ManageAccountDataAction(
        TelemetryEventMethod.manage_user_data,
        TelemetryEventObject.delete_user_data
    )
}